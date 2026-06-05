package com.example.paymentsystem.domain.order.service;

import com.example.paymentsystem.domain.cart.entity.CartItem;
import com.example.paymentsystem.domain.cart.repository.CartItemRepository;
import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.member.repository.MemberRepository;
import com.example.paymentsystem.domain.order.dto.CreateOrderRequest;
import com.example.paymentsystem.domain.order.dto.CreateOrderResponse;
import com.example.paymentsystem.domain.order.dto.OrderPreviewRequest;
import com.example.paymentsystem.domain.order.dto.OrderPreviewResponse;
import com.example.paymentsystem.domain.order.entity.Order;
import com.example.paymentsystem.domain.order.entity.OrderItem;
import com.example.paymentsystem.domain.order.repository.OrderItemRepository;
import com.example.paymentsystem.domain.order.repository.OrderRepository;
import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.repository.PaymentRepository;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

        // 주문에 사용한 장바구니 상품을 소비 처리한다.
        deleteOrderedCartItems(cartItems);

        return CreateOrderResponse.of(order, payment, pointAmount);
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
}