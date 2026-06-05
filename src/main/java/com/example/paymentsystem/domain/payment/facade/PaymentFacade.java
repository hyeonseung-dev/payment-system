package com.example.paymentsystem.domain.payment.facade;

import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.dto.PaymentConfirmResponse;
import com.example.paymentsystem.domain.payment.service.PaymentService;
import com.example.paymentsystem.domain.payment.service.PaymentCommandService;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import com.example.paymentsystem.infra.portone.client.PortOneClient;
import com.example.paymentsystem.infra.portone.dto.PortOneCancelResponse;
import com.example.paymentsystem.infra.portone.dto.PortOnePaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 결제 흐름을 조율하는 Facade 서비스이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private static final String PORTONE_PAID_STATUS = "PAID";
    private static final String PAYMENT_AMOUNT_MISMATCH_CANCEL_REASON = "결제 금액 불일치로 인한 자동 취소";

    private final PaymentService paymentService;
    private final PaymentCommandService paymentCommandService;
    private final PortOneClient portOneClient;

    /**
     * 결제를 확정한다.
     *
     * <p>주문과 결제를 조회하고 PortOne 결제 정보를 검증한 뒤 결제 완료 흐름을 조율한다.
     * Order, Point, Cart 처리는 각 도메인 기능과 함께 결제 확정 흐름에 연결한다.</p>
     *
     * @param memberId 인증 회원 ID
     * @param orderId 주문 ID
     * @param portonePaymentId PortOne 결제 식별자
     * @return 결제 확정 응답
     */
    public PaymentConfirmResponse confirmPayment(Long memberId, Long orderId, String portonePaymentId) {
        log.info("결제 확정 요청: memberId={}, orderId={}, portonePaymentId={}",
                memberId, orderId, portonePaymentId);

        // 주문 + 결제 조회
        Payment payment = paymentService.findByOrderIdWithOrder(orderId);
        log.info("주문 결제 조회 완료: orderId={}, paymentId={}, paymentStatus={}, orderStatus={}",
                orderId, payment.getId(), payment.getStatus(), payment.getOrder().getStatus());

        // 소유권 검증
        validateOwnership(payment, memberId);

        // 요청 portonePaymentId와 서버에 저장된 portonePaymentId 일치 여부 검증
        validatePortonePaymentId(payment, portonePaymentId);

        // 이미 결제 완료
        if (payment.isPaid()) {
            log.info("이미 완료된 결제 확정 요청: memberId={}, orderId={}, paymentId={}",
                    memberId, orderId, payment.getId());
            return PaymentConfirmResponse.from(payment);
        }

        // PortOne 결제 정보 조회
        if (payment.getPgAmount() > 0) {
            log.info("PortOne 결제 조회 시작: orderId={}, paymentId={}, portonePaymentId={}",
                    orderId, payment.getId(), portonePaymentId);
            PortOnePaymentResponse portOnePayment = portOneClient.getPayment(portonePaymentId);
            log.info("PortOne 결제 조회 완료: orderId={}, paymentId={}, portoneStatus={}, paidAmount={}",
                    orderId, payment.getId(), portOnePayment.status(), portOnePayment.paidAmount());

            // 결제 상태 성공 검증
            if (!isPortOnePaymentPaid(portOnePayment)) {
                // 주문취소 + 결제실패 + 재고복구
                log.warn("PortOne 결제 미완료: orderId={}, paymentId={}, portonePaymentId={}, portoneStatus={}",
                        orderId, payment.getId(), portonePaymentId, portOnePayment.status());
                paymentCommandService.failPaymentAndOrder(orderId);
                throw new BusinessException(ErrorCode.PAYMENT_NOT_PAID);
            }

            // 금액 일치 검증
            if (!isPaymentAmountMatched(portOnePayment, payment)) {
                // PG 보상취소 + 주문취소 + 재고복구
                log.warn("결제 금액 불일치: orderId={}, paymentId={}, expectedPgAmount={}, actualPaidAmount={}",
                        orderId, payment.getId(), payment.getPgAmount(), portOnePayment.paidAmount());
                cancelMismatchedPayment(portonePaymentId, portOnePayment);
                paymentCommandService.failPaymentAndOrder(orderId);
                throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
            }
        }

        PaymentConfirmResponse response = paymentCommandService.approvePaymentAndOrder(orderId);
        log.info("결제 확정 완료: memberId={}, orderId={}, paymentId={}, paymentStatus={}, orderStatus={}",
                memberId, response.orderId(), response.paymentId(), response.paymentStatus(), response.orderStatus());
        return response;
    }

    private void validateOwnership(Payment payment, Long memberId) {
        Long ownerId = payment.getOrder().getMember().getId();
        if (!Objects.equals(ownerId, memberId)) {
            log.warn("결제 주문 소유권 검증 실패: memberId={}, ownerId={}, orderId={}, paymentId={}",
                    memberId, ownerId, payment.getOrder().getId(), payment.getId());
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void validatePortonePaymentId(Payment payment, String portonePaymentId) {
        if (!payment.getPortonePaymentId().equals(portonePaymentId)) {
            log.warn("PortOne 결제 식별자 불일치: orderId={}, paymentId={}, expectedPortonePaymentId={}, requestPortonePaymentId={}",
                    payment.getOrder().getId(), payment.getId(), payment.getPortonePaymentId(), portonePaymentId);
            throw new BusinessException(ErrorCode.PAYMENT_ID_MISMATCH);
        }
    }

    private boolean isPortOnePaymentPaid(PortOnePaymentResponse portOnePayment) {
        return PORTONE_PAID_STATUS.equals(portOnePayment.status());
    }

    private boolean isPaymentAmountMatched(PortOnePaymentResponse portOnePayment, Payment payment) {
        return payment.getPgAmount().equals(portOnePayment.paidAmount());
    }

    private void cancelMismatchedPayment(String portonePaymentId, PortOnePaymentResponse portOnePayment) {
        log.warn("금액 불일치 결제 PortOne 보상취소 요청: portonePaymentId={}, cancelAmount={}",
                portonePaymentId, portOnePayment.paidAmount());
        PortOneCancelResponse cancelResponse = portOneClient.cancelPayment(
                portonePaymentId,
                portOnePayment.paidAmount(),
                PAYMENT_AMOUNT_MISMATCH_CANCEL_REASON
        );
        log.warn("금액 불일치 결제 PortOne 보상취소 완료: portonePaymentId={}, cancelledAmount={}, cancelStatus={}",
                cancelResponse.portonePaymentId(), cancelResponse.cancelledAmount(), cancelResponse.status());
    }
}
