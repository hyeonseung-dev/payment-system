package com.example.paymentsystem.domain.refund.facade;

import com.example.paymentsystem.domain.refund.dto.RefundItemRequest;
import com.example.paymentsystem.domain.refund.dto.RefundResponse;
import com.example.paymentsystem.domain.refund.entity.RefundStatus;
import com.example.paymentsystem.domain.refund.service.RefundCommandService;
import com.example.paymentsystem.domain.refund.service.RefundCommandService.RequestedRefundResult;
import com.example.paymentsystem.infra.portone.client.PortOneClient;
import com.example.paymentsystem.infra.portone.dto.PortOneCancelResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 환불 Facade의 PortOne 호출 경계를 검증한다.
 *
 * <p>환불은 내부 REQUESTED 저장 이후 외부 PG 환불을 호출한다.
 * PG 실패 시 선점 수량을 FAILED로 풀어야 하고, 성공 시 내부 완료 처리를 이어가야 한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class RefundFacadeTest {

    @InjectMocks
    private RefundFacade refundFacade;

    @Mock
    private RefundCommandService refundCommandService;

    @Mock
    private PortOneClient portOneClient;

    @Test
    void 환불_요청은_REQUESTED_선점_후_최종_PG금액과_멱등성키로_PortOne을_호출한다() {
        // given
        RequestedRefundResult requestedRefund = new RequestedRefundResult(
                1L,
                "pay_1",
                1000L,
                "idem-1"
        );
        List<RefundItemRequest> items = List.of(new RefundItemRequest(10L, 1));
        RefundResponse response = RefundResponse.of(
                1L,
                1L,
                RefundStatus.COMPLETED,
                1000L,
                0L,
                1000L,
                0L,
                0L,
                com.example.paymentsystem.domain.payment.entity.PaymentStatus.REFUNDED
        );

        when(refundCommandService.requestRefund(1L, 1L, "단순 변심", items))
                .thenReturn(requestedRefund);
        when(portOneClient.cancelPayment("pay_1", 1000L, "단순 변심", "idem-1"))
                .thenReturn(new PortOneCancelResponse("pay_1", 1000L, "SUCCEEDED"));
        when(refundCommandService.completeRequestedRefund(1L)).thenReturn(response);

        // when
        refundFacade.refundPayment(1L, 1L, "단순 변심", items);

        // then
        verify(portOneClient).cancelPayment("pay_1", 1000L, "단순 변심", "idem-1");
        verify(refundCommandService).completeRequestedRefund(1L);
    }

    @Test
    void PortOne_환불_요청이_실패하면_REQUESTED_환불을_FAILED로_변경한다() {
        // given
        RequestedRefundResult requestedRefund = new RequestedRefundResult(
                1L,
                "pay_1",
                1000L,
                "idem-1"
        );
        List<RefundItemRequest> items = List.of(new RefundItemRequest(10L, 1));

        when(refundCommandService.requestRefund(1L, 1L, "단순 변심", items))
                .thenReturn(requestedRefund);
        when(portOneClient.cancelPayment("pay_1", 1000L, "단순 변심", "idem-1"))
                .thenThrow(new ResourceAccessException("network error"));

        // when & then
        assertThatThrownBy(() -> refundFacade.refundPayment(1L, 1L, "단순 변심", items))
                .isInstanceOf(ResourceAccessException.class);

        verify(refundCommandService).failRequestedRefund(1L);
        verify(refundCommandService, never()).completeRequestedRefund(1L);
    }
}
