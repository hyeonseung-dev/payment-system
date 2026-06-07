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
     * <p>PortOne 환불 요청 전에 내부 DB에서 REQUESTED 환불을 먼저 저장해 수량을 선점한다.
     * 이후 PortOne 환불이 성공하면 내부 상태를 완료 처리하고, 실패하면 선점한 환불을 실패 처리해
     * 해당 수량을 다시 환불 요청할 수 있게 한다.</p>
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

        // PortOne 호출 전에 결제 소유권, 환불 가능 상태, 환불 가능 수량을 검증하고 REQUESTED 환불로 수량을 선점한다.
        RequestedRefundResult requestedRefund = refundCommandService.requestRefund(
                memberId,
                paymentId,
                reason,
                items
        );

        try {
            // PG 환불액이 있는 경우에만 PortOne 환불 API를 호출한다. 포인트만 환불되는 경우 외부 API 호출은 생략한다.
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
            // PortOne 요청 실패 시 REQUESTED 선점 상태를 FAILED로 변경해 환불 가능 수량을 다시 열어준다.
            refundCommandService.failRequestedRefund(requestedRefund.refundId());
            throw e;
        }

        // PortOne 환불 성공 후에만 재고, 포인트, 결제/주문 상태를 실제로 변경한다.
        RefundResponse response;
        try {
            response = refundCommandService.completeRequestedRefund(requestedRefund.refundId());
        } catch (RuntimeException e) {
            // 이미 PortOne 환불이 성공했을 수 있으므로 FAILED로 바꾸지 않고 REQUESTED 상태를 유지해 수동 복구 대상으로 남긴다.
            log.error("PortOne 환불 성공 후 내부 완료 처리 실패: refundId={}, paymentId={}, portonePaymentId={}, idempotencyKey={}, pgRefundAmount={}",
                    requestedRefund.refundId(),
                    paymentId,
                    requestedRefund.portonePaymentId(),
                    requestedRefund.idempotencyKey(),
                    requestedRefund.pgRefundAmount(),
                    e);
            throw e;
        }
        log.info("환불 완료: memberId={}, paymentId={}, refundId={}, totalRefundAmount={}, paymentStatus={}",
                memberId, response.paymentId(), response.refundId(),
                response.totalRefundAmount(), response.paymentStatus());
        return response;
    }
}
