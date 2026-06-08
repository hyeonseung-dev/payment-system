package com.example.paymentsystem.domain.payment.facade;

import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.order.entity.Order;
import com.example.paymentsystem.domain.payment.dto.PaymentCancelResponse;
import com.example.paymentsystem.domain.payment.dto.PaymentConfirmResponse;
import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.service.PaymentCommandService;
import com.example.paymentsystem.domain.payment.service.PaymentService;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import com.example.paymentsystem.infra.portone.client.PortOneClient;
import com.example.paymentsystem.infra.portone.dto.PortOneCancelResponse;
import com.example.paymentsystem.infra.portone.dto.PortOnePaymentResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 결제 Facade의 핵심 위험 지점을 검증한다.
 *
 * <p>결제는 외부 PortOne 조회 결과와 내부 DB 결제 정보가 모두 맞아야 완료된다.
 * 따라서 금액 불일치, 결제 식별자 불일치, 타인 결제 접근, 중복 호출 멱등성을 우선 테스트한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    @InjectMocks
    private PaymentFacade paymentFacade;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentCommandService paymentCommandService;

    @Mock
    private PortOneClient portOneClient;

    @Test
    void 결제확정_성공_시_PortOne_상태와_금액을_검증하고_내부_완료처리를_호출한다() {
        // given
        Long memberId = 1L;
        Long orderId = 10L;
        Payment payment = createReadyPayment(memberId, orderId, 1000L, 1000L);
        PaymentConfirmResponse expectedResponse = PaymentConfirmResponse.from(payment);

        when(paymentService.findByOrderIdWithOrder(orderId)).thenReturn(payment);
        when(portOneClient.getPayment(payment.getPortonePaymentId()))
                .thenReturn(new PortOnePaymentResponse(payment.getPortonePaymentId(), "PAID", 1000L));
        when(paymentCommandService.approvePaymentAndOrder(orderId)).thenReturn(expectedResponse);

        // when
        PaymentConfirmResponse response = paymentFacade.confirmPayment(
                memberId,
                orderId,
                payment.getPortonePaymentId()
        );

        // then
        assertThat(response).isSameAs(expectedResponse);
        verify(portOneClient).getPayment(payment.getPortonePaymentId());
        verify(paymentCommandService).approvePaymentAndOrder(orderId);
    }

    @Test
    void PortOne_결제금액이_DB_PG금액과_다르면_보상취소_후_결제실패로_처리한다() {
        // given
        Long orderId = 10L;
        Payment payment = createReadyPayment(1L, orderId, 1000L, 1000L);

        when(paymentService.findByOrderIdWithOrder(orderId)).thenReturn(payment);
        when(portOneClient.getPayment(payment.getPortonePaymentId()))
                .thenReturn(new PortOnePaymentResponse(payment.getPortonePaymentId(), "PAID", 900L));
        when(portOneClient.cancelPayment(eq(payment.getPortonePaymentId()), eq(900L), anyString(), anyString()))
                .thenReturn(new PortOneCancelResponse(payment.getPortonePaymentId(), 900L, "SUCCEEDED"));

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirmPayment(1L, orderId, payment.getPortonePaymentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PAYMENT_AMOUNT_MISMATCH.getMessage());

        // 금액이 맞지 않는 실제 결제는 방치하지 않고 PortOne 보상취소와 내부 실패 처리를 시도해야 한다.
        verify(portOneClient).cancelPayment(eq(payment.getPortonePaymentId()), eq(900L), anyString(), anyString());
        verify(paymentCommandService).failPaymentAndOrder(orderId);
        verify(paymentCommandService, never()).approvePaymentAndOrder(orderId);
    }

    @Test
    void 요청_PortOne_결제식별자가_DB값과_다르면_PortOne_조회_전_실패한다() {
        // given
        Long orderId = 10L;
        Payment payment = createReadyPayment(1L, orderId, 1000L, 1000L);

        when(paymentService.findByOrderIdWithOrder(orderId)).thenReturn(payment);

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirmPayment(1L, orderId, "wrong_payment_id"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.PAYMENT_ID_MISMATCH.getMessage());

        verify(portOneClient, never()).getPayment(anyString());
        verify(paymentCommandService, never()).approvePaymentAndOrder(orderId);
    }

    @Test
    void 다른_회원의_결제확정_요청은_PortOne_조회_전_차단한다() {
        // given
        Long orderId = 10L;
        Payment payment = createReadyPayment(1L, orderId, 1000L, 1000L);

        when(paymentService.findByOrderIdWithOrder(orderId)).thenReturn(payment);

        // when & then
        assertThatThrownBy(() -> paymentFacade.confirmPayment(999L, orderId, payment.getPortonePaymentId()))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.FORBIDDEN.getMessage());

        verify(portOneClient, never()).getPayment(anyString());
        verify(paymentCommandService, never()).approvePaymentAndOrder(orderId);
    }

    @Test
    void 이미_PAID인_결제확정_요청은_PortOne을_다시_조회하지_않고_멱등하게_응답한다() {
        // given
        Long orderId = 10L;
        Payment payment = createReadyPayment(1L, orderId, 1000L, 1000L);
        payment.complete(LocalDateTime.now());

        when(paymentService.findByOrderIdWithOrder(orderId)).thenReturn(payment);

        // when
        PaymentConfirmResponse response = paymentFacade.confirmPayment(
                1L,
                orderId,
                payment.getPortonePaymentId()
        );

        // then
        assertThat(response.paymentStatus()).isEqualTo("PAID");
        verify(portOneClient, never()).getPayment(anyString());
        verify(paymentCommandService, never()).approvePaymentAndOrder(orderId);
    }

    @Test
    void 이미_REFUNDED인_결제에_취소_웹훅이_도착하면_실패하지_않고_멱등하게_응답한다() {
        // given
        Payment payment = createReadyPayment(1L, 10L, 1000L, 1000L);
        payment.complete(LocalDateTime.now());
        payment.markRefunded();

        when(paymentService.findByPortonePaymentIdWithOrderAndMember(payment.getPortonePaymentId()))
                .thenReturn(payment);

        // when
        PaymentCancelResponse response = paymentFacade.cancelPaymentFromWebhook(payment.getPortonePaymentId());

        // then
        assertThat(response.paymentStatus()).isEqualTo("REFUNDED");
        verify(portOneClient, never()).getPayment(anyString());
        verify(paymentCommandService, never()).cancelPaymentAndOrder(payment.getId());
    }

    private Payment createReadyPayment(Long memberId, Long orderId, Long totalAmount, Long pgAmount) {
        Member member = Member.create("test" + memberId + "@test.com", "password", "테스터", "010-1234-5678");
        setId(member, memberId);

        Order order = Order.createPending(member, "ORD-TEST-" + orderId, totalAmount.intValue(), 0);
        setId(order, orderId);

        Payment payment = Payment.createReady(order, "pay_" + orderId, totalAmount, pgAmount, 0L);
        setId(payment, orderId);
        return payment;
    }

    private void setId(Object target, Long id) {
        try {
            Field idField = target.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
