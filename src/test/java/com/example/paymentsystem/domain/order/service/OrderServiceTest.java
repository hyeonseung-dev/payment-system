package com.example.paymentsystem.domain.order.service;

import com.example.paymentsystem.domain.cart.entity.Cart;
import com.example.paymentsystem.domain.cart.entity.CartItem;
import com.example.paymentsystem.domain.cart.repository.CartItemRepository;
import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.member.repository.MemberRepository;
import com.example.paymentsystem.domain.order.dto.*;
import com.example.paymentsystem.domain.order.entity.Order;
import com.example.paymentsystem.domain.order.entity.OrderItem;
import com.example.paymentsystem.domain.order.repository.OrderItemRepository;
import com.example.paymentsystem.domain.order.repository.OrderRepository;
import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.entity.PaymentStatus;
import com.example.paymentsystem.domain.payment.repository.PaymentRepository;
import com.example.paymentsystem.domain.point.service.PointService;
import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.domain.product.repository.ProductRepository;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.paymentsystem.domain.order.enums.OrderStatus.PAYMENT_PENDING;
import static com.example.paymentsystem.domain.product.enumtype.ProductCategory.FOOD;
import static com.example.paymentsystem.domain.product.enumtype.ProductStatus.FOR_SALE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Spy
    private OrderValidator orderValidator;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private OrderNumberGenerator orderNumberGenerator;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private PointService pointService;

    Long memberId = 1L;
    Long cartItemId = 1L;
    Long productId = 1L;
    List<Long> cartItemIds = List.of(cartItemId);

    @Test
    void 인증_정보가_없으면_예외가_발생한다() {
        // given
        CreateOrderRequest request = new CreateOrderRequest(cartItemIds, 0);

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(null, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());

        verify(memberRepository, never()).findByIdWithLock(any());
        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void 회원을_찾을_수_없으면_예외가_발생한다() {
        // given
        CreateOrderRequest request = new CreateOrderRequest(cartItemIds, 0);

        when(memberRepository.findByIdWithLock(memberId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void 주문서_미리보기를_성공한다() {
        // given
        OrderPreviewRequest request = new OrderPreviewRequest(cartItemIds);
        CartItem cartItem = createCartItemFixture(cartItemId, productId, 500000, 30, 2);

        when(cartItemRepository.findAllByIdsAndMemberId(request.cartItemIds(), memberId))
                .thenReturn(List.of(cartItem));

        // when
        OrderPreviewResponse response = orderService.previewOrder(memberId, request);

        // then
        assertThat(response.items()).hasSize(1);
        assertThat(response.totalAmount()).isEqualTo(1000000);

        OrderPreviewItemResponse item = response.items().get(0);

        assertThat(item.cartItemId()).isEqualTo(cartItemId);
        assertThat(item.productId()).isEqualTo(productId);
        assertThat(item.productName()).isEqualTo("아이폰 케이스");
        assertThat(item.price()).isEqualTo(500000);
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.subtotal()).isEqualTo(1_000_000);
    }

    @Test
    void 조회된_장바구니_상품이_없으면_예외가_발생한다() {
        // given
        OrderPreviewRequest request = new OrderPreviewRequest(List.of(10L));

        when(cartItemRepository.findAllByIdsAndMemberId(request.cartItemIds(), memberId))
                .thenReturn(List.of());

        // when & then
        assertThatThrownBy(() -> orderService.previewOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CART_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    void 요청한_장바구니_상품_중_조회되지_않은_항목이_있으면_예외가_발생한다() {
        // given
        Long requestedCartItemId1 = 10L;
        Long requestedCartItemId2 = 20L;

        OrderPreviewRequest request = new OrderPreviewRequest(
                List.of(requestedCartItemId1, requestedCartItemId2)
        );

        CartItem cartItem = createCartItemFixture(cartItemId, productId, 500000, 30, 2);

        when(cartItemRepository.findAllByIdsAndMemberId(request.cartItemIds(), memberId))
                .thenReturn(List.of(cartItem));

        // when & then
        assertThatThrownBy(() -> orderService.previewOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.CART_ITEM_NOT_FOUND.getMessage());
    }

    @Test
    void 주문_생성에_성공하면_포인트와_재고를_선차감하고_결제대기_Payment를_생성한다() {
        // given
        CreateOrderRequest request = new CreateOrderRequest(cartItemIds, 5000);

        Member member = createMemberWithPoint(10000);
        Product product = createProduct(productId, "떡볶이", 15000, 10);
        Cart cart = Cart.create(member);
        CartItem cartItem = CartItem.create(cart, product, 2);
        Order order = mock(Order.class);
        Payment payment = mock(Payment.class);

        when(memberRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(member));
        when(cartItemRepository.findAllByIdsAndMemberIdWithLock(cartItemIds, memberId))
                .thenReturn(List.of(cartItem));
        when(orderNumberGenerator.generate()).thenReturn("ORD-20260605-000001");
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        when(order.getId()).thenReturn(1L);
        when(order.getOrderNumber()).thenReturn("ORD-20260605-000001");
        when(order.getTotalAmount()).thenReturn(30000);

        when(payment.getPortonePaymentId()).thenReturn("pay_1234567890");
        when(payment.getId()).thenReturn(1L);
        when(payment.getPgAmount()).thenReturn(25000L);

        // when
        CreateOrderResponse response = orderService.createOrder(memberId, request);

        // then
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.orderNumber()).isEqualTo("ORD-20260605-000001");
        assertThat(response.portonePaymentId()).isEqualTo("pay_1234567890");
        assertThat(response.totalAmount()).isEqualTo(30_000);
        assertThat(response.pointAmount()).isEqualTo(5_000);
        assertThat(response.pgAmount()).isEqualTo(25_000);

        assertThat(product.getStockQuantity()).isEqualTo(8);

        verify(orderValidator).validateCartItems(List.of(cartItem), cartItemIds);
        verify(orderValidator).validateStock(List.of(cartItem));
        verify(orderItemRepository).saveAll(anyList());
        verify(paymentRepository).save(any(Payment.class));
        verify(pointService).usePoint(memberId, 1L, 5_000);
    }

    @Test
    void 주문금액보다_많은_포인트를_사용하면_예외가_발생한다() {
        // given
        CreateOrderRequest request = new CreateOrderRequest(cartItemIds, 100000);

        Member member = createMemberWithPoint(200000);
        CartItem cartItem = createCartItemFixture(cartItemId, productId, 15000, 10, 2);

        when(memberRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(member));
        when(cartItemRepository.findAllByIdsAndMemberIdWithLock(cartItemIds, memberId))
                .thenReturn(List.of(cartItem));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_POINT_AMOUNT.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderItemRepository, never()).saveAll(anyList());
        verify(paymentRepository, never()).save(any(Payment.class));


    }

    @Test
    void 보유_포인트가_부족하면_예외가_발생한다() {
        // given
        CreateOrderRequest request = new CreateOrderRequest(cartItemIds, 5_000);

        Member member = createMemberWithPoint(1_000);
        CartItem cartItem = createCartItemFixture(cartItemId, productId, 15_000, 10, 2);

        when(memberRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(member));
        when(cartItemRepository.findAllByIdsAndMemberIdWithLock(cartItemIds, memberId))
                .thenReturn(List.of(cartItem));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_POINT.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void 상품_주문_생성에_성공하면_포인트와_재고를_차감하고_결제대기_Payment를_생성한다() {
        // given
        CreateProductOrderRequest request = new CreateProductOrderRequest(productId, 2, 5_000);

        Member member = createMemberWithPoint(10_000);
        Product product = createProduct(productId, "아이폰 케이스", 15_000, 10);

        Order order = mock(Order.class);
        Payment payment = mock(Payment.class);

        // 상품 바로 주문은 회원과 상품을 비관적 락으로 조회한다.
        when(memberRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(member));
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));

        // 주문번호 생성 결과를 고정해서 응답 검증을 쉽게 한다.
        when(orderNumberGenerator.generate()).thenReturn("ORD-20260605-000001");

        // 저장된 주문과 결제 객체를 mock으로 반환한다.
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        // 응답 DTO 생성에 필요한 주문 값이다.
        when(order.getId()).thenReturn(1L);
        when(order.getOrderNumber()).thenReturn("ORD-20260605-000001");
        when(order.getStatus()).thenReturn(PAYMENT_PENDING);
        when(order.getTotalAmount()).thenReturn(30_000);

        // 응답 DTO 생성에 필요한 결제 값이다.
        when(payment.getPortonePaymentId()).thenReturn("pay_12345678");
        when(payment.getId()).thenReturn(1L);
        when(payment.getPgAmount()).thenReturn(25_000L);

        // when
        CreateOrderResponse response = orderService.createProductOrder(memberId, request);

        // then
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.orderNumber()).isEqualTo("ORD-20260605-000001");
        assertThat(response.portonePaymentId()).isEqualTo("pay_12345678");
        assertThat(response.orderStatus()).isEqualTo(PAYMENT_PENDING);
        assertThat(response.totalAmount()).isEqualTo(30_000);
        assertThat(response.pointAmount()).isEqualTo(5_000);
        assertThat(response.pgAmount()).isEqualTo(25_000);

        // 재고가 실제로 차감됐는지 확인한다. 포인트 차감과 이력 저장은 PointService가 담당한다.
        assertThat(product.getStockQuantity()).isEqualTo(8);

        // 상품 바로 주문은 장바구니를 사용하지 않는다.
        verify(cartItemRepository, never()).deleteAll(anyList());

        // 주문 상품 스냅샷과 결제대기 Payment가 저장됐는지 확인한다.
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(paymentRepository).save(any(Payment.class));
        verify(pointService).usePoint(memberId, 1L, 5_000);
    }

    @Test
    void 상품_주문_생성_시_상품을_찾을_수_없으면_예외가_발생한다() {
        // given
        CreateProductOrderRequest request = new CreateProductOrderRequest(productId, 2, 0);

        Member member = createMemberWithPoint(10_000);

        when(memberRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(member));
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.createProductOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PRODUCT_NOT_FOUND.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void 주문_목록_조회에_성공한다() {
        // given
        int page = 0;
        int size = 10;

        Order order = mock(Order.class);
        OrderItem orderItem1 = mock(OrderItem.class);
        OrderItem orderItem2 = mock(OrderItem.class);

        Pageable pageable = PageRequest.of(page, size);

        // 주문 목록 조회 결과로 사용할 주문 데이터다.
        when(order.getId()).thenReturn(1L);
        when(order.getOrderNumber()).thenReturn("ORD-20260605-000001");
        when(order.getStatus()).thenReturn(PAYMENT_PENDING);
        when(order.getTotalAmount()).thenReturn(30_000);
        when(order.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 6, 5, 16, 30));

        // 주문 상품을 주문 ID 기준으로 묶기 위해 OrderItem에서 Order를 꺼낼 수 있어야 한다.
        when(orderItem1.getOrder()).thenReturn(order);
        when(orderItem2.getOrder()).thenReturn(order);

        when(orderRepository.findByMember_IdOrderByCreatedAtDesc(memberId, pageable))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        when(orderItemRepository.findAllByOrderIds(List.of(1L)))
                .thenReturn(List.of(orderItem1, orderItem2));

        // when
        OrderListResponse response = orderService.findOrder(memberId, page, size);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);

        OrderListItemResponse item = response.content().get(0);

        assertThat(item.orderId()).isEqualTo(1L);
        assertThat(item.orderNumber()).isEqualTo("ORD-20260605-000001");
        assertThat(item.status()).isEqualTo(PAYMENT_PENDING);
        assertThat(item.itemCount()).isEqualTo(2);
        assertThat(item.totalAmount()).isEqualTo(30_000);
        assertThat(item.createAt()).isEqualTo(LocalDateTime.of(2026, 6, 5, 16, 30));

        verify(orderRepository).findByMember_IdOrderByCreatedAtDesc(memberId, pageable);
        verify(orderItemRepository).findAllByOrderIds(List.of(1L));
    }

    @Test
    void 주문_목록이_없으면_빈_목록을_반환한다() {
        // given
        int page = 0;
        int size = 10;

        Pageable pageable = PageRequest.of(page, size);

        // 주문이 없는 상황은 예외가 아니라 빈 페이지로 응답한다.
        when(orderRepository.findByMember_IdOrderByCreatedAtDesc(memberId, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // when
        OrderListResponse response = orderService.findOrder(memberId, page, size);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(0);
        assertThat(response.totalPages()).isEqualTo(0);

        // 주문이 없으면 주문 상품 조회는 하지 않아도 된다.
        verify(orderItemRepository, never()).findAllByOrderIds(anyList());
    }

    @Test
    void 주문_목록_조회_시_인증_정보가_없으면_예외가_발생한다() {
        // given
        int page = 0;
        int size = 10;

        // when & then
        assertThatThrownBy(() -> orderService.findOrder(null, page, size))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());

        verify(orderRepository, never()).findByMember_IdOrderByCreatedAtDesc(anyLong(), any(Pageable.class));
        verify(orderItemRepository, never()).findAllByOrderIds(anyList());
    }

    @Test
    void 주문_목록_조회_시_페이지_요청값이_잘못되면_예외가_발생한다() {
        // given
        int page = -1;
        int size = 10;

        // when & then
        assertThatThrownBy(() -> orderService.findOrder(memberId, page, size))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_INPUT.getMessage());

        verify(orderRepository, never()).findByMember_IdOrderByCreatedAtDesc(anyLong(), any(Pageable.class));
        verify(orderItemRepository, never()).findAllByOrderIds(anyList());
    }

    private CartItem createCartItemFixture(
            Long cartItemId,
            Long productId,
            int price,
            int stockQuantity,
            int quantity
    ) {
        // 주문 테스트에 필요한 회원, 상품, 장바구니, 장바구니 상품을 한 번에 만든다.
        Member member = createMemberWithPoint(10_000);
        Product product = createProduct(productId, "아이폰 케이스", price, stockQuantity);
        Cart cart = Cart.create(member);
        CartItem cartItem = CartItem.create(cart, product, quantity);

        setId(cartItem, cartItemId);

        return cartItem;
    }

    @Test
    void 상품_주문_생성_시_재고가_부족하면_예외가_발생한다() {
        // given
        CreateProductOrderRequest request = new CreateProductOrderRequest(productId, 20, 0);

        Member member = createMemberWithPoint(10_000);
        Product product = createProduct(productId, "아이폰 케이스", 15_000, 10);

        when(memberRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(member));
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> orderService.createProductOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_STOCK.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void 상품_주문_금액보다_많은_포인트를_사용하면_예외가_발생한다() {
        // given
        CreateProductOrderRequest request = new CreateProductOrderRequest(productId, 2, 40_000);

        Member member = createMemberWithPoint(100_000);
        Product product = createProduct(productId, "아이폰 케이스", 15_000, 10);

        when(memberRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(member));
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> orderService.createProductOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_POINT_AMOUNT.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void 상품_주문_생성_시_보유_포인트가_부족하면_예외가_발생한다() {
        // given
        CreateProductOrderRequest request = new CreateProductOrderRequest(productId, 2, 5_000);

        Member member = createMemberWithPoint(1_000);
        Product product = createProduct(productId, "아이폰 케이스", 15_000, 10);

        when(memberRepository.findByIdWithLock(memberId)).thenReturn(Optional.of(member));
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> orderService.createProductOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_POINT.getMessage());

        verify(orderRepository, never()).save(any(Order.class));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void 주문_상세_조회에_성공한다() {
        // given
        Long orderId = 1L;

        Order order = mock(Order.class);
        OrderItem orderItem = mock(OrderItem.class);
        Payment payment = mock(Payment.class);

        // 주문 상세 응답에 필요한 주문 정보를 준비한다.
        when(order.getId()).thenReturn(orderId);
        when(order.getOrderNumber()).thenReturn("ORD-20260605-000001");
        when(order.getStatus()).thenReturn(PAYMENT_PENDING);
        when(order.getTotalAmount()).thenReturn(30_000);
        when(order.getUsePointAmountSnapshot()).thenReturn(5_000);
        when(order.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 6, 7, 13, 0));

        // 주문 상세 응답에 필요한 주문 상품 스냅샷 정보를 준비한다.
        when(orderItem.getId()).thenReturn(1L);
        when(orderItem.getProductId()).thenReturn(1L);
        when(orderItem.getProductNameSnapshot()).thenReturn("아이폰 케이스");
        when(orderItem.getProductPriceSnapshot()).thenReturn(15_000);
        when(orderItem.getQuantity()).thenReturn(2);

        // 주문 상세 응답에 필요한 결제 정보를 준비한다.
        when(payment.getId()).thenReturn(10L);
        when(payment.getStatus()).thenReturn(PaymentStatus.READY);
        when(payment.getEarnedPointAmount()).thenReturn(0L);
        when(payment.getPgAmount()).thenReturn(25000L);

        // 주문 ID와 회원 ID가 모두 일치하는 주문만 조회된다.
        when(orderRepository.findByIdAndMember_Id(orderId, memberId))
                .thenReturn(Optional.of(order));

        // 주문 상품과 결제 정보를 조회한다.
        when(orderItemRepository.findAllByOrder_IdOrderByIdAsc(orderId))
                .thenReturn(List.of(orderItem));
        when(paymentRepository.findByOrder_Id(orderId))
                .thenReturn(Optional.of(payment));

        // when
        OrderDetailResponse response = orderService.findOrderDetail(memberId, orderId);

        // then
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.orderNumber()).isEqualTo("ORD-20260605-000001");
        assertThat(response.status()).isEqualTo(PAYMENT_PENDING);
        assertThat(response.totalAmount()).isEqualTo(30_000);
        assertThat(response.pointAmount()).isEqualTo(5_000);
        assertThat(response.pgAmount()).isEqualTo(25_000L);
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(response.earnedPointAmount()).isEqualTo(0L);
        assertThat(response.paymentId()).isEqualTo(10L);
        assertThat(response.items()).hasSize(1);

        OrderDetailItemResponse item = response.items().get(0);

        assertThat(item.orderItemId()).isEqualTo(1L);
        assertThat(item.productId()).isEqualTo(1L);
        assertThat(item.productName()).isEqualTo("아이폰 케이스");
        assertThat(item.price()).isEqualTo(15_000);
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.subtotal()).isEqualTo(30_000);
    }

    @Test
    void 주문_상세_조회_시_인증_정보가_없으면_예외가_발생한다() {
        // given
        Long orderId = 1L;

        // when & then
        assertThatThrownBy(() -> orderService.findOrderDetail(null, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());

        // 인증 정보가 없으면 DB 조회를 하지 않는다.
        verify(orderRepository, never()).findByIdAndMember_Id(anyLong(), anyLong());
        verify(orderItemRepository, never()).findAllByOrder_IdOrderByIdAsc(anyLong());
        verify(paymentRepository, never()).findByOrder_Id(anyLong());
    }

    @Test
    void 주문_상세_조회_시_주문이_없거나_내_주문이_아니면_예외가_발생한다() {
        // given
        Long orderId = 1L;

        // 주문 ID와 회원 ID가 일치하는 주문이 없으면 빈 Optional을 반환한다.
        when(orderRepository.findByIdAndMember_Id(orderId, memberId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.findOrderDetail(memberId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());

        // 주문이 없으면 주문 상품과 결제 정보는 조회하지 않는다.
        verify(orderItemRepository, never()).findAllByOrder_IdOrderByIdAsc(anyLong());
        verify(paymentRepository, never()).findByOrder_Id(anyLong());
    }

    @Test
    void 주문_상세_조회_시_결제_정보가_없으면_예외가_발생한다() {
        // given
        Long orderId = 1L;

        Order order = mock(Order.class);

        when(orderRepository.findByIdAndMember_Id(orderId, memberId))
                .thenReturn(Optional.of(order));

        // 주문 상품은 조회됐지만 결제 정보가 없는 상황이다.
        when(orderItemRepository.findAllByOrder_IdOrderByIdAsc(orderId))
                .thenReturn(List.of());
        when(paymentRepository.findByOrder_Id(orderId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.findOrderDetail(memberId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PAYMENT_NOT_FOUND.getMessage());
    }
    @Test
    void 결제대기_주문_취소에_성공한다() {
        // given
        Long orderId = 1L;

        Member member = createMemberWithPoint(0);
        Product product = createProduct(productId, "아이폰 케이스", 15_000, 8);

        Order order = Order.createPending(member, "ORD-20260608-000001", 30_000, 5_000);
        setId(order, orderId);

        OrderItem orderItem = OrderItem.createProductSnapshot(order, product, 2);
        setId(orderItem, 1L);

        Payment payment = Payment.createReady(order, "pay_12345678", 30_000L, 25_000L, 0L);
        setId(payment, 1L);

        when(orderRepository.findByIdAndMember_Id(orderId, memberId))
                .thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_Id(orderId))
                .thenReturn(Optional.of(payment));
        when(orderItemRepository.findAllByOrder_IdOrderByIdAsc(orderId))
                .thenReturn(List.of(orderItem));
        when(productRepository.findById(productId))
                .thenReturn(Optional.of(product));

        // when
        OrderCancelResponse response = orderService.cancelPendingOrder(memberId, orderId);

        // then
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.orderNumber()).isEqualTo("ORD-20260608-000001");
        assertThat(response.orderStatus().name()).isEqualTo("CANCELED");
        assertThat(response.paymentStatus()).isEqualTo(PaymentStatus.FAILED);

        // 주문 취소 시 선차감했던 재고가 복구되어야 한다.
        assertThat(product.getStockQuantity()).isEqualTo(10);

        // 주문 생성 시 사용했던 포인트도 PointHistory와 함께 복구되어야 한다.
        verify(pointService).cancelUsePoint(memberId, payment.getId(), 5_000);
    }

    @Test
    void 주문_취소_시_인증_정보가_없으면_예외가_발생한다() {
        // given
        Long orderId = 1L;

        // when & then
        assertThatThrownBy(() -> orderService.cancelPendingOrder(null, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.UNAUTHORIZED.getMessage());

        verify(orderRepository, never()).findByIdAndMember_Id(anyLong(), anyLong());
        verify(paymentRepository, never()).findByOrder_Id(anyLong());
    }

    @Test
    void 주문_취소_시_주문이_없거나_내_주문이_아니면_예외가_발생한다() {
        // given
        Long orderId = 1L;

        when(orderRepository.findByIdAndMember_Id(orderId, memberId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.cancelPendingOrder(memberId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());

        verify(paymentRepository, never()).findByOrder_Id(anyLong());
        verify(orderItemRepository, never()).findAllByOrder_IdOrderByIdAsc(anyLong());
    }

    @Test
    void 주문_취소_시_결제_정보가_없으면_예외가_발생한다() {
        // given
        Long orderId = 1L;

        Member member = createMemberWithPoint(0);
        Order order = Order.createPending(member, "ORD-20260608-000001", 30_000, 0);
        setId(order, orderId);

        when(orderRepository.findByIdAndMember_Id(orderId, memberId))
                .thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_Id(orderId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.cancelPendingOrder(memberId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PAYMENT_NOT_FOUND.getMessage());

        verify(orderItemRepository, never()).findAllByOrder_IdOrderByIdAsc(anyLong());
    }

    @Test
    void 결제대기_상태가_아닌_주문은_취소할_수_없다() {
        // given
        Long orderId = 1L;

        Member member = createMemberWithPoint(0);
        Order order = Order.createPending(member, "ORD-20260608-000001", 30_000, 0);
        setId(order, orderId);

        // 결제 완료 상태로 변경해서 취소 불가능 상태를 만든다.
        order.completePayment(0);

        Payment payment = Payment.createReady(order, "pay_12345678", 30_000L, 30_000L, 0L);
        setId(payment, 1L);

        when(orderRepository.findByIdAndMember_Id(orderId, memberId))
                .thenReturn(Optional.of(order));
        when(paymentRepository.findByOrder_Id(orderId))
                .thenReturn(Optional.of(payment));

        // when & then
        assertThatThrownBy(() -> orderService.cancelPendingOrder(memberId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_ORDER_STATUS.getMessage());

        verify(orderItemRepository, never()).findAllByOrder_IdOrderByIdAsc(anyLong());
    }


    private Product createProduct(Long id, String name, int price, int stockQuantity) {
        // 테스트용 상품을 만든다.
        Product product = Product.create(
                name,
                price,
                stockQuantity,
                FOOD,
                FOR_SALE
        );

        setId(product, id);

        return product;
    }

    private Member createMemberWithPoint(int pointBalance) {
        // 포인트 선차감 테스트를 위해 보유 포인트가 있는 회원을 만든다.
        return new Member(
                "test@test.com",
                "password",
                "테스터",
                "010-1234-5678",
                pointBalance
        );
    }

    private void setId(Object target, Long id) {
        try {
            // 응답 DTO에서 id를 검증하기 위해 테스트 객체에 id를 주입한다.
            Field idField = target.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
