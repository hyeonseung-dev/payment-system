package com.example.paymentsystem.domain.refund.facade;

import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.service.PaymentService;
import com.example.paymentsystem.domain.refund.dto.RefundItemRequest;
import com.example.paymentsystem.domain.refund.dto.RefundResponse;
import com.example.paymentsystem.domain.refund.service.RefundCommandService;
import com.example.paymentsystem.domain.refund.service.RefundService;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import com.example.paymentsystem.infra.portone.client.PortOneClient;
import com.example.paymentsystem.infra.portone.dto.PortOneCancelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 환불 흐름을 조율하는 Facade 서비스이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundFacade {

    private final PaymentService paymentService;
    private final RefundService refundService;
    private final RefundCommandService refundCommandService;
    private final PortOneClient portOneClient;

    /**
     * 결제에 포함된 주문 상품을 환불한다.
     *
     * <p>환불 대상 결제와 소유권을 검증하고 PortOne 환불 요청을 수행한 뒤,
     * 내부 DB 상태 변경은 {@link RefundCommandService}에 위임한다.</p>
     *
     * @param memberId 인증 회원 ID
     * @param paymentId 결제 ID
     * @param reason 환불 사유
     * @param items 환불할 주문 상품 목록
     * @return 환불 응답
     */
    public RefundResponse refundPayment(
            Long memberId,
            Long paymentId,
            String reason,
            List<RefundItemRequest> items
    ) {
        log.info("환불 요청: memberId={}, paymentId={}", memberId, paymentId);

        Payment payment = paymentService.findByIdWithOrderAndMember(paymentId);
        log.info("환불 대상 조회 완료: orderId={}, paymentId={}, paymentStatus={}",
                payment.getOrder().getId(), paymentId, payment.getStatus());

        // 결제id와 회원id가 일치하는지 검증
        validateOwnership(payment, memberId);

        // 결제 상태가 환불이 가능한 상태인지 검증
        refundService.validateRefundablePayment(payment);

        Long pgRefundAmount = refundCommandService.calculatePgRefundAmount(paymentId, items);
        if (pgRefundAmount > 0) {
            log.info("PortOne 환불 요청: orderId={}, paymentId={}, portonePaymentId={}, pgRefundAmount={}",
                    payment.getOrder().getId(), paymentId, payment.getPortonePaymentId(), pgRefundAmount);
            PortOneCancelResponse cancelResponse = portOneClient.cancelPayment(
                    payment.getPortonePaymentId(),
                    pgRefundAmount,
                    reason
            );
            log.info("PortOne 환불 완료: orderId={}, paymentId={}, cancelledAmount={}, cancelStatus={}",
                    payment.getOrder().getId(), paymentId, cancelResponse.cancelledAmount(), cancelResponse.status());
        }

        RefundResponse response = refundCommandService.completeRefund(paymentId, reason, items);
        log.info("환불 완료: memberId={}, paymentId={}, refundId={}, totalRefundAmount={}, paymentStatus={}",
                memberId, response.paymentId(), response.refundId(),
                response.totalRefundAmount(), response.paymentStatus());
        return response;
    }

    private void validateOwnership(Payment payment, Long memberId) {
        Long ownerId = payment.getOrder().getMember().getId();
        if (!Objects.equals(ownerId, memberId)) {
            log.warn("환불 결제 소유권 검증 실패: memberId={}, ownerId={}, orderId={}, paymentId={}",
                    memberId, ownerId, payment.getOrder().getId(), payment.getId());
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
