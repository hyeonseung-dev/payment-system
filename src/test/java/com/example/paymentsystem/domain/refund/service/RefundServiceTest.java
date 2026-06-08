package com.example.paymentsystem.domain.refund.service;

import com.example.paymentsystem.domain.order.entity.OrderItem;
import com.example.paymentsystem.domain.refund.entity.RefundStatus;
import com.example.paymentsystem.domain.refund.repository.RefundItemRepository;
import com.example.paymentsystem.domain.refund.repository.RefundRepository;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 환불 가능 수량 계산 규칙을 검증한다.
 *
 * <p>REQUESTED는 아직 PG 환불 완료 전이지만 중복 요청을 막기 위해 선점 수량으로 본다.
 * COMPLETED는 실제 환불 완료 수량이고, FAILED는 실패한 요청이므로 다시 환불 가능해야 한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class RefundServiceTest {

    @InjectMocks
    private RefundService refundService;

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private RefundItemRepository refundItemRepository;

    @Test
    void 환불_수량은_REQUESTED와_COMPLETED만_선점_수량으로_계산한다() {
        // given
        Long orderItemId = 1L;
        when(refundItemRepository.sumQuantityByOrderItemIdAndRefundStatuses(
                org.mockito.ArgumentMatchers.eq(orderItemId),
                org.mockito.ArgumentMatchers.anyList()
        )).thenReturn(2L);

        // when
        int reservedQuantity = refundService.calculateReservedRefundQuantity(orderItemId);

        // then
        assertThat(reservedQuantity).isEqualTo(2);

        ArgumentCaptor<List<RefundStatus>> statusesCaptor = ArgumentCaptor.forClass(List.class);
        verify(refundItemRepository).sumQuantityByOrderItemIdAndRefundStatuses(
                org.mockito.ArgumentMatchers.eq(orderItemId),
                statusesCaptor.capture()
        );

        assertThat(statusesCaptor.getValue())
                .containsExactly(RefundStatus.REQUESTED, RefundStatus.COMPLETED)
                .doesNotContain(RefundStatus.FAILED);
    }

    @Test
    void REQUESTED와_COMPLETED_선점_수량을_제외한_남은_수량보다_많이_환불하면_실패한다() {
        // given
        OrderItem orderItem = mock(OrderItem.class);
        when(orderItem.getId()).thenReturn(1L);
        when(orderItem.getQuantity()).thenReturn(3);
        when(refundItemRepository.sumQuantityByOrderItemIdAndRefundStatuses(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.anyList()
        )).thenReturn(2L);

        // when & then
        assertThatThrownBy(() -> refundService.validateRefundQuantity(orderItem, 2))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.REFUND_QUANTITY_EXCEEDED.getMessage());
    }

    @Test
    void FAILED_환불은_선점_수량에서_제외되므로_다시_환불_요청할_수_있다() {
        // given
        OrderItem orderItem = mock(OrderItem.class);
        when(orderItem.getId()).thenReturn(1L);
        when(orderItem.getQuantity()).thenReturn(3);

        // Repository가 REQUESTED + COMPLETED만 합산하므로 FAILED 2건이 있어도 여기서는 0으로 조회된다.
        when(refundItemRepository.sumQuantityByOrderItemIdAndRefundStatuses(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.anyList()
        )).thenReturn(0L);

        // when & then
        refundService.validateRefundQuantity(orderItem, 3);
    }
}
