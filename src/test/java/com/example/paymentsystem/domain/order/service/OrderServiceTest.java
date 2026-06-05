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
import com.example.paymentsystem.domain.payment.repository.PaymentRepository;
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

import java.lang.reflect.Field;
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

        assertThat(member.getPointBalance()).isEqualTo(5_000);
        assertThat(product.getStockQuantity()).isEqualTo(8);

        verify(orderValidator).validateCartItems(List.of(cartItem), cartItemIds);
        verify(orderValidator).validateStock(List.of(cartItem));
        verify(orderItemRepository).saveAll(anyList());
        verify(paymentRepository).save(any(Payment.class));
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

        // 포인트와 재고가 실제로 차감됐는지 확인한다.
        assertThat(member.getPointBalance()).isEqualTo(5_000);
        assertThat(product.getStockQuantity()).isEqualTo(8);

        // 상품 바로 주문은 장바구니를 사용하지 않는다.
        verify(cartItemRepository, never()).deleteAll(anyList());

        // 주문 상품 스냅샷과 결제대기 Payment가 저장됐는지 확인한다.
        verify(orderItemRepository).save(any(OrderItem.class));
        verify(paymentRepository).save(any(Payment.class));
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