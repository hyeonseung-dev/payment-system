package com.example.paymentsystem.domain.payment.facade;

import com.example.paymentsystem.domain.payment.dto.PaymentCancelResponse;
import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.dto.PaymentConfirmResponse;
import com.example.paymentsystem.domain.payment.entity.PaymentStatus;
import com.example.paymentsystem.domain.payment.service.PaymentService;
import com.example.paymentsystem.domain.payment.service.PaymentCommandService;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import com.example.paymentsystem.infra.portone.client.PortOneClient;
import com.example.paymentsystem.infra.portone.client.PortOneIdempotencyKeyGenerator;
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
    private static final String PORTONE_CANCELLED_STATUS = "CANCELLED";
    private static final String PORTONE_CANCELED_STATUS = "CANCELED";
    private static final String PAYMENT_AMOUNT_MISMATCH_CANCEL_REASON = "결제 금액 불일치로 인한 자동 취소";
    private static final String ALREADY_CANCELLED_MESSAGE = "이미 취소된 결제입니다.";
    private static final String ALREADY_CANCELLED_OR_REFUNDED_MESSAGE = "이미 취소 또는 환불된 결제입니다.";

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

    /**
     * 웹훅으로 수신한 결제를 확정한다.
     *
     * <p>웹훅은 JWT 인증 회원이 없으므로 소유권 검증을 수행하지 않는다.
     * 대신 서버에 저장된 Payment를 PortOne 결제 식별자로 조회하고, PortOne API 재조회 결과의
     * 결제 상태와 금액을 검증한 뒤 기존 결제 완료 처리 흐름을 호출한다.</p>
     *
     * @param portonePaymentId PortOne 결제 식별자
     * @return 결제 확정 응답
     */
    public PaymentConfirmResponse confirmPaymentFromWebhook(String portonePaymentId) {
        log.info("웹훅 결제 확정 요청: portonePaymentId={}", portonePaymentId);

        // PortOne 결제 식별자로 결제 + 주문 조회
        Payment payment = paymentService.findByPortonePaymentIdWithOrderAndMember(portonePaymentId);
        Long orderId = payment.getOrder().getId();
        log.info("웹훅 결제 조회 완료: orderId={}, paymentId={}, paymentStatus={}, orderStatus={}",
                orderId, payment.getId(), payment.getStatus(), payment.getOrder().getStatus());

        // 이미 결제 완료
        if (payment.isPaid()) {
            log.info("이미 완료된 웹훅 결제 확정 요청: orderId={}, paymentId={}", orderId, payment.getId());
            return PaymentConfirmResponse.from(payment);
        }

        // PortOne 결제 정보 재조회
        if (payment.getPgAmount() > 0) {
            log.info("웹훅 PortOne 결제 조회 시작: orderId={}, paymentId={}, portonePaymentId={}",
                    orderId, payment.getId(), portonePaymentId);
            PortOnePaymentResponse portOnePayment = portOneClient.getPayment(portonePaymentId);
            log.info("웹훅 PortOne 결제 조회 완료: orderId={}, paymentId={}, portoneStatus={}, paidAmount={}",
                    orderId, payment.getId(), portOnePayment.status(), portOnePayment.paidAmount());

            // 결제 상태 성공 검증
            if (!isPortOnePaymentPaid(portOnePayment)) {
                log.warn("웹훅 PortOne 결제 미완료: orderId={}, paymentId={}, portonePaymentId={}, portoneStatus={}",
                        orderId, payment.getId(), portonePaymentId, portOnePayment.status());
                paymentCommandService.failPaymentAndOrder(orderId);
                throw new BusinessException(ErrorCode.PAYMENT_NOT_PAID);
            }

            // 금액 일치 검증
            if (!isPaymentAmountMatched(portOnePayment, payment)) {
                log.warn("웹훅 결제 금액 불일치: orderId={}, paymentId={}, expectedPgAmount={}, actualPaidAmount={}",
                        orderId, payment.getId(), payment.getPgAmount(), portOnePayment.paidAmount());
                cancelMismatchedPayment(portonePaymentId, portOnePayment);
                paymentCommandService.failPaymentAndOrder(orderId);
                throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
            }
        }

        PaymentConfirmResponse response = paymentCommandService.approvePaymentAndOrder(orderId);
        log.info("웹훅 결제 확정 완료: orderId={}, paymentId={}, paymentStatus={}, orderStatus={}",
                response.orderId(), response.paymentId(), response.paymentStatus(), response.orderStatus());
        return response;
    }

    /**
     * 결제를 전체 취소한다.
     *
     * <p>결제 소유권과 취소 가능 상태를 검증한 뒤 PortOne 결제취소를 요청하고,
     * 내부 결제 상태를 전체 환불 상태로 변경한다.</p>
     *
     * @param memberId 인증 회원 ID
     * @param paymentId 결제 ID
     * @param reason 결제취소 사유
     * @return 결제취소 응답
     */
    public PaymentCancelResponse cancelPayment(Long memberId, Long paymentId, String reason) {
        log.info("결제취소 요청: memberId={}, paymentId={}, reason={}", memberId, paymentId, reason);

        // 결제 + 주문 조회
        Payment payment = paymentService.findByIdWithOrderAndMember(paymentId);
        log.info("결제취소 대상 조회 완료: orderId={}, paymentId={}, paymentStatus={}, orderStatus={}",
                payment.getOrder().getId(), payment.getId(), payment.getStatus(), payment.getOrder().getStatus());

        // 소유권 검증
        validateOwnership(payment, memberId);

        // 이미 결제취소 완료
        if (payment.isCancelled()) {
            log.info("이미 취소된 결제 요청: memberId={}, orderId={}, paymentId={}",
                    memberId, payment.getOrder().getId(), payment.getId());
            return PaymentCancelResponse.of(payment, ALREADY_CANCELLED_MESSAGE);
        }

        // 결제취소 가능 상태 검증
        validateCancelablePayment(payment);

        Long cancelAmount = payment.getPgAmount();

        // PortOne 결제취소 요청
        if (cancelAmount > 0) {
            log.info("PortOne 결제취소 요청: orderId={}, paymentId={}, portonePaymentId={}, cancelAmount={}",
                    payment.getOrder().getId(), payment.getId(), payment.getPortonePaymentId(), cancelAmount);
            PortOneCancelResponse cancelResponse = portOneClient.cancelPayment(
                    payment.getPortonePaymentId(),
                    cancelAmount,
                    reason,
                    PortOneIdempotencyKeyGenerator.generateIdempotencyKey()
            );
            log.info("PortOne 결제취소 완료: orderId={}, paymentId={}, cancelledAmount={}, cancelStatus={}",
                    payment.getOrder().getId(), payment.getId(), cancelResponse.cancelledAmount(), cancelResponse.status());
        }

        return paymentCommandService.cancelPaymentAndOrder(paymentId);
    }

    /**
     * 웹훅으로 수신한 결제취소 상태를 내부 결제 상태에 반영한다.
     *
     * <p>취소 웹훅은 PortOne에서 이미 취소가 발생한 뒤 전달되는 알림이므로
     * PortOne 취소 API를 다시 호출하지 않는다. PortOne 결제 정보를 재조회해 취소 상태를 확인하고,
     * 내부 Payment가 아직 PAID 상태인 경우에만 취소 상태로 변경한다.</p>
     *
     * @param portonePaymentId PortOne 결제 식별자
     * @return 결제취소 응답
     */
    public PaymentCancelResponse cancelPaymentFromWebhook(String portonePaymentId) {
        log.info("웹훅 결제취소 동기화 요청: portonePaymentId={}", portonePaymentId);

        // PortOne 결제 식별자로 결제 + 주문 조회
        Payment payment = paymentService.findByPortonePaymentIdWithOrderAndMember(portonePaymentId);
        Long orderId = payment.getOrder().getId();
        log.info("웹훅 결제취소 대상 조회 완료: orderId={}, paymentId={}, paymentStatus={}, orderStatus={}",
                orderId, payment.getId(), payment.getStatus(), payment.getOrder().getStatus());

        // 이미 결제취소 완료
        if (payment.isCancelled()) {
            log.info("이미 취소된 웹훅 결제취소 요청: orderId={}, paymentId={}", orderId, payment.getId());
            return PaymentCancelResponse.of(payment, ALREADY_CANCELLED_MESSAGE);
        }

        // 환불 API에서 이미 전액 환불 처리된 뒤 PortOne 취소 웹훅이 늦게 도착한 경우 멱등하게 완료로 본다.
        if (payment.isRefunded()) {
            log.info("이미 전액 환불된 웹훅 결제취소 요청 무시: orderId={}, paymentId={}", orderId, payment.getId());
            return PaymentCancelResponse.of(payment, ALREADY_CANCELLED_OR_REFUNDED_MESSAGE);
        }

        // PortOne 결제 정보 재조회
        if (payment.getPgAmount() > 0) {
            log.info("웹훅 PortOne 결제취소 상태 조회 시작: orderId={}, paymentId={}, portonePaymentId={}",
                    orderId, payment.getId(), portonePaymentId);
            PortOnePaymentResponse portOnePayment = portOneClient.getPayment(portonePaymentId);
            log.info("웹훅 PortOne 결제취소 상태 조회 완료: orderId={}, paymentId={}, portoneStatus={}, paidAmount={}",
                    orderId, payment.getId(), portOnePayment.status(), portOnePayment.paidAmount());

            if (!isPortOnePaymentCancelled(portOnePayment)) {
                log.warn("웹훅 PortOne 결제취소 상태 불일치: orderId={}, paymentId={}, portonePaymentId={}, portoneStatus={}",
                        orderId, payment.getId(), portonePaymentId, portOnePayment.status());
                throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
            }
        }

        validateCancelablePayment(payment);
        PaymentCancelResponse response = paymentCommandService.cancelPaymentAndOrder(payment.getId());
        log.info("웹훅 결제취소 동기화 완료: orderId={}, paymentId={}, paymentStatus={}, orderStatus={}",
                response.orderId(), response.paymentId(), response.paymentStatus(), response.orderStatus());
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

    private void validateCancelablePayment(Payment payment) {
        if (payment.getStatus() != PaymentStatus.PAID) {
            log.warn("결제취소 불가능 상태: orderId={}, paymentId={}, paymentStatus={}",
                    payment.getOrder().getId(), payment.getId(), payment.getStatus());
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS);
        }
    }

    private boolean isPortOnePaymentPaid(PortOnePaymentResponse portOnePayment) {
        return PORTONE_PAID_STATUS.equals(portOnePayment.status());
    }

    private boolean isPortOnePaymentCancelled(PortOnePaymentResponse portOnePayment) {
        return PORTONE_CANCELLED_STATUS.equals(portOnePayment.status())
                || PORTONE_CANCELED_STATUS.equals(portOnePayment.status());
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
                PAYMENT_AMOUNT_MISMATCH_CANCEL_REASON,
                PortOneIdempotencyKeyGenerator.generateIdempotencyKey()
        );
        log.warn("금액 불일치 결제 PortOne 보상취소 완료: portonePaymentId={}, cancelledAmount={}, cancelStatus={}",
                cancelResponse.portonePaymentId(), cancelResponse.cancelledAmount(), cancelResponse.status());
    }
}
