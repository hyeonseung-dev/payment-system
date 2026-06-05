package com.example.paymentsystem.domain.payment.service;

import com.example.paymentsystem.domain.payment.dto.PaymentConfirmResponse;
import com.example.paymentsystem.domain.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

    private final PaymentService paymentService;

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

        // Payment 실패
        paymentService.failPayment(payment);
        log.warn("Payment 실패 처리 완료: orderId={}, paymentId={}", orderId, payment.getId());

        // 주문취소
        // TODO: OrderService 구현 후 주문 취소 처리 연결

        // 재고복구
        // TODO: OrderItem 조회와 Product 재고 복구 처리 연결

        // 사용 포인트 복구
        // TODO: 주문 생성 또는 결제 확정 시 포인트 사용 정책 확정 후 복구 처리 연결
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
        // TODO: OrderService 구현 후 주문 완료 처리 연결

        // Payment 완료
        paymentService.confirmPayment(payment, LocalDateTime.now());
        log.info("Payment 완료 처리 완료: orderId={}, paymentId={}", orderId, payment.getId());

        // 포인트 적립
        // TODO: PointService 구현 후 pgAmount 기준 포인트 적립 처리 연결

        // 장바구니 비우기
        // TODO: CartService 구현 후 회원 장바구니 비우기 처리 연결

        PaymentConfirmResponse response = PaymentConfirmResponse.from(payment);
        log.info("결제 승인 처리 완료: orderId={}, paymentId={}, paymentStatus={}, orderStatus={}",
                response.orderId(), response.paymentId(), response.paymentStatus(), response.orderStatus());
        return response;
    }
}
