package com.example.paymentsystem.domain.payment.service;

import com.example.paymentsystem.domain.cart.service.CartService;
import com.example.paymentsystem.domain.order.entity.OrderItem;
import com.example.paymentsystem.domain.order.repository.OrderItemRepository;
import com.example.paymentsystem.domain.payment.dto.PaymentCancelResponse;
import com.example.paymentsystem.domain.payment.dto.PaymentConfirmResponse;
import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.entity.PaymentStatus;
import com.example.paymentsystem.domain.point.service.PointService;
import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.domain.product.repository.ProductRepository;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 결제 확정과 실패 시 여러 도메인의 DB 상태 변경을 묶어 처리하는 서비스이다.
 *
 * <p>PortOne 같은 외부 API 호출은 Facade에서 수행하고,
 * 이 서비스는 내부 DB 상태 변경만 트랜잭션으로 처리한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCommandService {

    private static final String PAYMENT_CANCELLED_MESSAGE = "결제가 취소되었습니다.";

    private final PaymentService paymentService;
    private final CartService cartService;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final PointService pointService;

    /**
     * 결제 실패에 따른 내부 상태 변경을 처리한다.
     *
     * <p>결제가 완료되지 않았거나 결제 검증에 실패한 경우 호출한다.
     * Payment는 실패 상태로 변경하고, 주문 취소와 재고 복구는 각 도메인 기능과 연결한다.</p>
     *
     * @param orderId 주문 ID
     */
    @Transactional
    public void failPaymentAndOrder(Long orderId) {
        Payment payment = paymentService.findByOrderIdWithOrder(orderId);
        log.warn("결제 실패 처리 시작: orderId={}, paymentId={}, paymentStatus={}, orderStatus={}",
                orderId, payment.getId(), payment.getStatus(), payment.getOrder().getStatus());

        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.warn("이미 실패 처리된 결제 요청 무시: orderId={}, paymentId={}", orderId, payment.getId());
            return;
        }

        if (!payment.isReady()) {
            log.warn("결제 실패 처리 불가능 상태: orderId={}, paymentId={}, paymentStatus={}",
                    orderId, payment.getId(), payment.getStatus());
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 주문취소
        payment.getOrder().cancel();
        log.warn("Order 취소 처리 완료: orderId={}, paymentId={}", orderId, payment.getId());

        // 재고복구
        List<OrderItem> orderItems = orderItemRepository.findAllByOrder_IdOrderByIdAsc(orderId);
        restoreStock(orderItems);
        log.warn("결제 실패 재고 복구 완료: orderId={}, paymentId={}", orderId, payment.getId());

        // 사용 포인트 복구
        restoreUsedPoint(payment);
        log.warn("결제 실패 사용 포인트 복구 완료: orderId={}, paymentId={}", orderId, payment.getId());

        // Payment 실패
        paymentService.failPayment(payment);
        log.warn("Payment 실패 처리 완료: orderId={}, paymentId={}", orderId, payment.getId());
    }

    /**
     * 결제 승인에 따른 내부 상태 변경을 처리한다.
     *
     * <p>PortOne 결제 상태와 금액 검증이 완료된 뒤 호출한다.
     * Payment는 완료 상태로 변경하고, 주문 완료와 포인트 적립, 장바구니 비우기는 각 도메인 기능과 연결한다.</p>
     *
     * @param orderId 주문 ID
     * @return 결제 확정 응답
     */
    @Transactional
    public PaymentConfirmResponse approvePaymentAndOrder(Long orderId) {
        Payment payment = paymentService.findByOrderIdWithOrder(orderId);
        log.info("결제 승인 처리 시작: orderId={}, paymentId={}, paymentStatus={}, orderStatus={}",
                orderId, payment.getId(), payment.getStatus(), payment.getOrder().getStatus());

        // 주문완료
        payment.getOrder().completePayment(payment.getOrder().getUsePointAmountSnapshot());
        log.info("Order 완료 처리 완료: orderId={}, paymentId={}", orderId, payment.getId());

        // Payment 완료
        paymentService.confirmPayment(payment, LocalDateTime.now());
        log.info("Payment 완료 처리 완료: orderId={}, paymentId={}", orderId, payment.getId());

        // 포인트 적립
        earnPoint(payment);
        log.info("결제 포인트 적립 처리 완료: orderId={}, paymentId={}, earnedPointAmount={}",
                orderId, payment.getId(), payment.getEarnedPointAmount());

        // 주문에 사용된 장바구니 항목 삭제
        cartService.deleteOrderedCartItemsByOrderId(orderId);
        log.info("주문 장바구니 항목 삭제 완료: orderId={}, paymentId={}", orderId, payment.getId());

        PaymentConfirmResponse response = PaymentConfirmResponse.from(payment);
        log.info("결제 승인 처리 완료: orderId={}, paymentId={}, paymentStatus={}, orderStatus={}",
                response.orderId(), response.paymentId(), response.paymentStatus(), response.orderStatus());
        return response;
    }

    /**
     * 결제취소에 따른 내부 상태 변경을 처리한다.
     *
     * <p>PortOne 결제취소가 완료된 뒤 호출한다.
     * Payment와 Order는 취소 상태로 변경하고, 재고/포인트 복구는 각 도메인 기능과 연결한다.</p>
     *
     * @param paymentId 결제 ID
     * @return 결제취소 응답
     */
    @Transactional
    public PaymentCancelResponse cancelPaymentAndOrder(Long paymentId) {
        Payment payment = paymentService.findByIdWithOrderAndMember(paymentId);
        log.info("결제취소 내부 처리 시작: orderId={}, paymentId={}, paymentStatus={}, orderStatus={}",
                payment.getOrder().getId(), payment.getId(), payment.getStatus(), payment.getOrder().getStatus());

        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            log.info("이미 취소 처리된 결제 요청 무시: orderId={}, paymentId={}",
                    payment.getOrder().getId(), payment.getId());
            return PaymentCancelResponse.of(payment, PAYMENT_CANCELLED_MESSAGE);
        }

        if (!payment.isPaid()) {
            log.warn("결제취소 내부 처리 불가능 상태: orderId={}, paymentId={}, paymentStatus={}",
                    payment.getOrder().getId(), payment.getId(), payment.getStatus());
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }

        // 주문취소
        payment.getOrder().cancelPaidOrder();
        log.info("Order 결제취소 처리 완료: orderId={}, paymentId={}",
                payment.getOrder().getId(), payment.getId());

        // 재고복구
        List<OrderItem> orderItems = orderItemRepository.findAllByOrder_IdOrderByIdAsc(payment.getOrder().getId());
        restoreStock(orderItems);
        log.info("결제취소 재고 복구 완료: orderId={}, paymentId={}",
                payment.getOrder().getId(), payment.getId());

        // 포인트 복구/회수
        restoreUsedPoint(payment);
        cancelEarnedPoint(payment);
        log.info("결제취소 포인트 복구/회수 완료: orderId={}, paymentId={}",
                payment.getOrder().getId(), payment.getId());

        // Payment 취소
        paymentService.cancelPayment(payment);
        log.info("Payment 취소 처리 완료: orderId={}, paymentId={}",
                payment.getOrder().getId(), payment.getId());

        Payment cancelledPayment = paymentService.findByIdWithOrderAndMember(paymentId);
        PaymentCancelResponse response = PaymentCancelResponse.of(
                cancelledPayment,
                PAYMENT_CANCELLED_MESSAGE
        );
        log.info("결제취소 내부 처리 완료: orderId={}, paymentId={}, paymentStatus={}",
                response.orderId(), response.paymentId(), response.paymentStatus());
        return response;
    }

    private void restoreStock(List<OrderItem> orderItems) {
        for (OrderItem orderItem : orderItems) {
            Product product = productRepository.findByIdWithLock(orderItem.getProductId()).orElseThrow(
                    () -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
            );
            product.increaseStock(orderItem.getQuantity());
        }
    }

    private void earnPoint(Payment payment) {
        Long earnedPointAmount = payment.getEarnedPointAmount();
        if (earnedPointAmount <= 0) {
            return;
        }

        pointService.earnPoint(
                payment.getOrder().getMember().getId(),
                payment.getId(),
                earnedPointAmount.intValue()
        );
    }

    private void restoreUsedPoint(Payment payment) {
        int usePointAmount = payment.getOrder().getUsePointAmountSnapshot();
        if (usePointAmount <= 0) {
            return;
        }

        pointService.cancelUsePoint(
                payment.getOrder().getMember().getId(),
                payment.getId(),
                usePointAmount
        );
    }

    private void cancelEarnedPoint(Payment payment) {
        Long earnedPointAmount = payment.getEarnedPointAmount();
        if (earnedPointAmount <= 0) {
            return;
        }

        pointService.cancelEarnPoint(
                payment.getOrder().getMember().getId(),
                payment.getId(),
                earnedPointAmount.intValue()
        );
    }
}
