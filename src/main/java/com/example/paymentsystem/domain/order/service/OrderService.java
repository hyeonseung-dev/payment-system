package com.example.paymentsystem.domain.order.service;

import com.example.paymentsystem.domain.cart.entity.CartItem;
import com.example.paymentsystem.domain.cart.repository.CartItemRepository;
import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.member.repository.MemberRepository;
import com.example.paymentsystem.domain.order.dto.*;
import com.example.paymentsystem.domain.order.entity.Order;
import com.example.paymentsystem.domain.order.entity.OrderItem;
import com.example.paymentsystem.domain.order.enums.OrderStatus;
import com.example.paymentsystem.domain.order.repository.OrderItemRepository;
import com.example.paymentsystem.domain.order.repository.OrderRepository;
import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.repository.PaymentRepository;
import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.domain.product.repository.ProductRepository;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartItemRepository cartItemRepository;
    private final OrderValidator orderValidator;
    private final MemberRepository memberRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public OrderPreviewResponse previewOrder(Long memberId, OrderPreviewRequest request) {

        List<CartItem> cartItems = cartItemRepository.findAllByIdsAndMemberId(request.cartItemIds(), memberId);

        // 요청한 장바구니 상품이 모두 조회되었는지 검증
        orderValidator.validateCartItems(cartItems, request.cartItemIds());

        return OrderPreviewResponse.from(cartItems);
    }

    @Transactional
    public CreateOrderResponse createOrder(Long memberId, CreateOrderRequest request) {
        Member member = findMemberWithLock(memberId);

        // 주문 생성 중 같은 장바구니 상품을 동시에 주문하지 못하도록 비관적 락을 건다.
        List<CartItem> cartItems = cartItemRepository.findAllByIdsAndMemberIdWithLock(request.cartItemIds(), memberId);

        // 요청한 장바구니 상품이 모두 유효한지 검증
        orderValidator.validateCartItems(cartItems, request.cartItemIds());

        // 주문 수량만큼 재고가 충분한지 검증
        orderValidator.validateStock(cartItems);

        // 주문 총 금액을 계산
        int totalAmount = calculateTotalAmount(cartItems);

        // TODO 포인트 도메인 구현 후 PointService를 통해 포인트 차감과 PointHistory저장을 함께 처리하도록 변경 예정
        // 포인트 사용 가능 여부를 검증하고 주문 생성 시점에 선차감한다.
        int pointAmount = request.getUsePointAmount();
        usePoint(member, totalAmount, pointAmount);

        // PG사에 실제 결제할 금액을 계산
        int pgAmount = calculatePgAmount(totalAmount, pointAmount);

        // 재고 검증이 끝난 뒤 실제 재고 선차감
        decreaseStock(cartItems);

        // 결제대기 주문을 생성
        Order order = createPendingOrder(member, totalAmount, pointAmount);

        // 주문 당시 상품 정보를 스냅샷으로 저장
        saveOrderItems(order, cartItems);

        // PortOne 결제에 사용할 결제대기 Payment를 생성
        Payment payment = createReadyPayment(order, totalAmount, pgAmount);

        return CreateOrderResponse.of(order, payment, pointAmount);
    }

    @Transactional
    public CreateOrderResponse createProductOrder(Long memberId, CreateProductOrderRequest request) {
        Member member = findMemberWithLock(memberId);

        // 재고 선차감을 위해 상품 row에 비관적 락을 건다.
        Product product = productRepository.findByIdWithLock(request.productId()).orElseThrow(
                () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 요청 수량만큼 재고가 충분한지 검증한다.
        orderValidator.validateProductStock(product, request.quantity());

        // 상품 가격과 요청 수량 기준으로 주문 총 금액을 계산
        int totalAmount = product.getPrice() * request.quantity();

        // 사용할 포인트를 꺼내 요청에 없으면 0
        int pointAmount = request.getUsePointAmount();

        // 포인트 사용 가능 여부를 검증하고 선차감
        usePoint(member, totalAmount, pointAmount);

        // PG사에 실제 결제할 금액을 계산
        int pgAmount = calculatePgAmount(totalAmount, pointAmount);

        // 검증이 끝났면 실제 상품 재고 선차감
        product.decreaseStock(request.quantity());

        // 결제대기 주문 생성
        Order order = createPendingOrder(member, totalAmount, pointAmount);

        // 주문 당시 상품명, 가격, 수량을 주문 상품 스냅샷으로 저장
        OrderItem orderItem = OrderItem.createProductSnapshot(order, product, request.quantity());
        orderItemRepository.save(orderItem);

        // PortOne 결제에 사용할 결제대기 Payment 생성
        Payment payment = createReadyPayment(order, totalAmount, pgAmount);

        return CreateOrderResponse.of(order, payment, pointAmount);
    }

    @Transactional(readOnly = true)
    public OrderListResponse findOrder(Long memberId, int page, int size) {
        // JWT 인증 정보가 없으면 주문 목록을 조회할 수 없다.
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        if (page < 0 || size < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        Pageable pageable = PageRequest.of(page, size);

        // 로그인한 회원의 주문만 최신순으로 조회한다.
        Page<Order> orderPage = orderRepository.findByMember_IdOrderByCreatedAtDesc(memberId, pageable);

        List<Order> orders = orderPage.getContent();

        // 주문이 없으면 빈 목록을 반환한다.
        if (orders.isEmpty()) {
            return OrderListResponse.of(
                    List.of(),
                    orderPage.getNumber(),
                    orderPage.getSize(),
                    orderPage.getTotalElements(),
                    orderPage.getTotalPages()
            );
        }

        // 주문 ID 목록을 만든다.
        List<Long> list = orders.stream()
                .map(Order::getId)
                .toList();

        // 주문 상품을 한 번에 조회해서 N+1 문제를 피한다.
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderIds(list);

        // 주문 ID 기준으로 주문 상품을 묶는다.
        Map<Long, List<OrderItem>> orderItemMap = orderItems.stream()
                .collect(Collectors.groupingBy(orderItem -> orderItem.getOrder().getId()));

        // 주문 목록 응답 DTO로 변환한다.
        List<OrderListItemResponse> content = orders.stream()
                .map(order -> OrderListItemResponse.from(
                        order,
                        orderItemMap.getOrDefault(order.getId(), List.of())
                ))
                .toList();

        return OrderListResponse.of(
                content,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public OrderDetailResponse findOrderDetail(Long memberId, Long orderId) {
        // JWT 인증 정보가 없으면 주문 상세를 조회할 수 없다.
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 주문이 없거나 다른 회원의 주문이면 모두 ORDER_NOT_FOUND로 처리
        Order order = orderRepository.findByIdAndMember_Id(orderId, memberId).orElseThrow(
                () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND)
        );

        // 주문 상품 목록은 주문 상세 응답에 필요하므로 별도로 조회
        List<OrderItem> orderItems = orderItemRepository.findAllByOrder_IdOrderByIdAsc(orderId);

        Payment payment = paymentRepository.findByOrder_Id(orderId).orElseThrow(
                () -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND)
        );

        return OrderDetailResponse.of(order, orderItems, payment);
    }

    @Transactional
    public OrderCancelResponse cancelPendingOrder(Long memberId, Long orderId) {
        // JWT 인증 정보가 없으면 주문을 취소할 없다.
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 주문이 없거나 다른 회원의 주문이면 모두 ORDER_NOT_FOUND로 처리
        Order order = orderRepository.findByIdAndMember_Id(orderId, memberId).orElseThrow(
                () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND)
        );

        // 주문에 연결된 결제 정보를 조회한다.
        Payment payment = paymentRepository.findByOrder_Id(orderId).orElseThrow(
                () -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND)
        );

        // 결제대기 주문과 결제대기 Payment만 취소할 수 있다.
        validateCancelable(order, payment);

        // 주문 상품 목록을 조회한다.
        List<OrderItem> orderItems = orderItemRepository.findAllByOrder_IdOrderByIdAsc(orderId);

        // 주문 생성 때 선차감한 재고를 복구
        restoreStock(orderItems);

        // 주문 생성 때 선처감한 포인트를 복구
        restorePoint(order);

        // 주문 상태를 CANCELED로 변경
        // 동시에 두 취소 요청이 들어오면 Order의 @Version으로 하나만 성공하면 나머지는 롤백
        order.cancel();

        // 결제대기 Payment는 실제 결제가 진행되지 않았으므로 FAILED로 변경
        payment.fail();

        return OrderCancelResponse.of(order, payment);
    }

    private void usePoint(Member member, int totalAmount, int pointAmount) {
        // 주문 금액보다 많은 포인트는 사용할 수 없다.
        if (pointAmount > totalAmount) {
            throw new BusinessException(ErrorCode.INVALID_POINT_AMOUNT);
        }

        // TODO 포인트 도메인 구현 후 member,usePoint 직접 호출 대신 PointService.usePoint(..)로 교체 예정
        // TODO 포인트 사용 이력을 같은 트랜잭션에서 저장해야 한다.
        // TODO OrderService가 PointService에 직접 의존하지 않도록 추후 OrderFacade에서 주문/포인트 흐름을 조합할 예정

        // 포인트를 사용하는 경우에만 회원 포인트를 선차감한다.
        if (pointAmount > 0) {
            member.usePoint(pointAmount);
        }
    }

    private void decreaseStock(List<CartItem> cartItems) {
        // 검증이 끝난 상품들의 재고를 실제로 차감
        for (CartItem cartItem : cartItems) {
            cartItem.getProduct().decreaseStock(cartItem.getQuantity());
        }
    }

    private int calculatePgAmount(int totalAmount, int pointAmount) {
        // PG사에 실제로 결제 요청할 금액이다.
        return totalAmount - pointAmount;
    }

    private int calculateTotalAmount(List<CartItem> cartItems) {
        // 주문 시점의 상품 가격과 수량 기준으로 총 주문 금액을 계산
        return cartItems.stream()
                .mapToInt(cartItem -> cartItem.getProduct().getPrice() * cartItem.getQuantity())
                .sum();
    }

    private Order createPendingOrder(Member member, int totalAmount, int pointAmount) {
        // 주문번호 생성 책임은 별도 컴포넌트에 위임한다.
        String orderNumber = orderNumberGenerator.generate();

        // 결제대기 주문 생성 책임은 Order 엔티티에 둔다.
        Order order = Order.createPending(member, orderNumber, totalAmount, pointAmount);

        return orderRepository.save(order);
    }

    private void saveOrderItems(Order order, List<CartItem> cartItems) {
        // 장바구니 상품을 주문 상품 스냅샷으로 변환한다.
        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> OrderItem.createSnapshot(order, cartItem))
                .toList();

        orderItemRepository.saveAll(orderItems);
    }

    private Payment createReadyPayment(Order order, int totalAmount, int pgAmount) {

        // PortOne 결제창에서 사용할 결제 식별자를 생성
        String portonePaymentId = createPortonePaymentId();

        // todo 포인트 적립 예정 로직 구현 후 수정 예정
        // 아직 결제 성공 전이므로 적립 예정 포인트는 0으로 둔다.
        long earnedPointAmount = 0L;

        Payment payment = Payment.createReady(
                order,
                portonePaymentId,
                (long) totalAmount,
                (long) pgAmount,
                earnedPointAmount
        );

        return paymentRepository.save(payment);
    }

    private String createPortonePaymentId() {
        // UUID를 이용해 중복 가능성이 낮은 결제 식별자를 만든다.
        return "pay_" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8);
    }

    private Member findMemberWithLock(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return memberRepository.findByIdWithLock(memberId).orElseThrow(
                () -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
        );
    }

    private void deleteOrderedCartItems(List<CartItem> cartItems) {
        // 주문에 사용된 장바구니 상품은 다시 주문되지 않도록 삭제한다.
        cartItemRepository.deleteAll(cartItems);
    }

    private void validateCancelable(Order order, Payment payment) {
        // 결제대기 주문만 최소할 수 있다.
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS);
        }

        // 결제 정보도 READY 상태여야 결제대기 주문 취소 가능
        if (!payment.isReady()) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }

    private void restoreStock(List<OrderItem> orderItems) {
        for (OrderItem orderItem : orderItems) {
            // OrderItem에는 Product 연관관계가 없으니 productId로 상품을 다시 조회
            Product product = productRepository.findById(orderItem.getProductId()).orElseThrow(
                    () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            );

            // 주문 생성 시 선차감했던 수량만큼 재고를 복구
            product.increaseStock(orderItem.getQuantity());
        }
    }

    private void restorePoint(Order order) {
        int pointAmount = order.getUsePointAmountSnapshot();

        // 포인트를 사용한 주문일 때만 포인트를 복구
        if (pointAmount > 0) {
            order.getMember().restoreUsePoint(pointAmount);
        }
    }

}
