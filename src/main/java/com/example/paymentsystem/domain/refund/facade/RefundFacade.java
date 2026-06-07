package com.example.paymentsystem.domain.refund.facade;

import com.example.paymentsystem.domain.refund.dto.RefundItemRequest;
import com.example.paymentsystem.domain.refund.dto.RefundResponse;
import com.example.paymentsystem.domain.refund.service.RefundCommandService;
import com.example.paymentsystem.domain.refund.service.RefundCommandService.RequestedRefundResult;
import com.example.paymentsystem.infra.portone.client.PortOneClient;
import com.example.paymentsystem.infra.portone.dto.PortOneCancelResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 환불 흐름을 조율하는 Facade 서비스이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundFacade {

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

        RequestedRefundResult requestedRefund = refundCommandService.requestRefund(
                memberId,
                paymentId,
                reason,
                items
        );

        try {
            if (requestedRefund.pgRefundAmount() > 0) {
                log.info("PortOne 환불 요청: paymentId={}, refundId={}, portonePaymentId={}, pgRefundAmount={}",
                        paymentId, requestedRefund.refundId(),
                        requestedRefund.portonePaymentId(), requestedRefund.pgRefundAmount());
                PortOneCancelResponse cancelResponse = portOneClient.cancelPayment(
                        requestedRefund.portonePaymentId(),
                        requestedRefund.pgRefundAmount(),
                        reason,
                        requestedRefund.idempotencyKey()
                );
                log.info("PortOne 환불 완료: paymentId={}, refundId={}, cancelledAmount={}, cancelStatus={}",
                        paymentId, requestedRefund.refundId(),
                        cancelResponse.cancelledAmount(), cancelResponse.status());
            }
        } catch (RuntimeException e) {
            refundCommandService.failRequestedRefund(requestedRefund.refundId());
            throw e;
        }

        RefundResponse response = refundCommandService.completeRequestedRefund(requestedRefund.refundId());
        log.info("환불 완료: memberId={}, paymentId={}, refundId={}, totalRefundAmount={}, paymentStatus={}",
                memberId, response.paymentId(), response.refundId(),
                response.totalRefundAmount(), response.paymentStatus());
        return response;
    }
}
