package com.example.paymentsystem.domain.refund.service;

import com.example.paymentsystem.domain.member.entity.Member;
import com.example.paymentsystem.domain.order.entity.Order;
import com.example.paymentsystem.domain.order.entity.OrderItem;
import com.example.paymentsystem.domain.order.repository.OrderItemRepository;
import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.entity.PaymentStatus;
import com.example.paymentsystem.domain.payment.service.PaymentService;
import com.example.paymentsystem.domain.product.entity.Product;
import com.example.paymentsystem.domain.product.repository.ProductRepository;
import com.example.paymentsystem.domain.refund.dto.RefundItemRequest;
import com.example.paymentsystem.domain.refund.dto.RefundResponse;
import com.example.paymentsystem.domain.refund.entity.Refund;
import com.example.paymentsystem.domain.refund.entity.RefundItem;
import com.example.paymentsystem.domain.refund.entity.RefundStatus;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.paymentsystem.domain.product.enumtype.ProductCategory.FOOD;
import static com.example.paymentsystem.domain.product.enumtype.ProductStatus.FOR_SALE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 환불 Command 서비스의 금액 계산과 상태 전이를 검증한다.
 *
 * <p>이 서비스는 환불 요청을 REQUESTED로 선점하고, PortOne 성공 후 재고/포인트/결제 상태를
 * 변경한다. 특히 PG 환불액 차감 정책이 들어와도 item별 금액이 음수가 되지 않아야 한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class RefundCommandServiceTest {

    @InjectMocks
    private RefundCommandService refundCommandService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RefundService refundService;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Test
    void 환불금액_배분_시_item별_PG와_포인트_합계가_전체금액과_일치하고_음수가_발생하지_않는다() {
        // given
        Payment payment = createPaidPayment(1L, 10L, 10_000L, 10_000L, 5_000L, 0);
        Refund refund = Refund.createRequested(payment, "단순 변심", 10_000L, 0L, 5_000L, 5_000L, 5_000L, "idem");
        setId(refund, 100L);

        OrderItem firstItem = createOrderItem(payment.getOrder(), 11L, 101L, 5_000, 1);
        OrderItem secondItem = createOrderItem(payment.getOrder(), 12L, 102L, 5_000, 1);
        List<RefundItemRequest> items = List.of(
                new RefundItemRequest(11L, 1),
                new RefundItemRequest(12L, 1)
        );

        when(paymentService.findByIdWithOrderAndMemberForUpdate(1L)).thenReturn(payment);
        when(orderItemRepository.findById(11L)).thenReturn(Optional.of(firstItem));
        when(orderItemRepository.findById(12L)).thenReturn(Optional.of(secondItem));
        when(refundService.calculateCompletedTotalRefundAmount(payment.getId())).thenReturn(0L);
        when(refundService.createRequestedRefund(
                eq(payment),
                eq("단순 변심"),
                eq(10_000L),
                eq(0L),
                eq(5_000L),
                eq(5_000L),
                eq(5_000L),
                anyString()
        )).thenReturn(refund);

        // when
        refundCommandService.requestRefund(1L, 1L, "단순 변심", items);

        // then
        ArgumentCaptor<Long> pointCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> pgCaptor = ArgumentCaptor.forClass(Long.class);
        verify(refundService, org.mockito.Mockito.times(2)).createRefundItem(
                eq(refund),
                any(OrderItem.class),
                eq(1),
                pointCaptor.capture(),
                pgCaptor.capture()
        );

        List<Long> pointAmounts = pointCaptor.getAllValues();
        List<Long> pgAmounts = pgCaptor.getAllValues();

        assertThat(pointAmounts).allMatch(amount -> amount >= 0);
        assertThat(pgAmounts).allMatch(amount -> amount >= 0);
        assertThat(pointAmounts.stream().mapToLong(Long::longValue).sum()).isEqualTo(0L);
        assertThat(pgAmounts.stream().mapToLong(Long::longValue).sum()).isEqualTo(5_000L);
    }

    @Test
    void 같은_요청에_동일_orderItemId가_중복되면_수량검증_우회를_막기_위해_실패한다() {
        // given
        Payment payment = createPaidPayment(1L, 10L, 10_000L, 10_000L, 0L, 0);
        List<RefundItemRequest> items = List.of(
                new RefundItemRequest(11L, 1),
                new RefundItemRequest(11L, 1)
        );

        when(paymentService.findByIdWithOrderAndMemberForUpdate(1L)).thenReturn(payment);

        // when & then
        assertThatThrownBy(() -> refundCommandService.requestRefund(1L, 1L, "중복 요청", items))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_INPUT.getMessage());

        verify(orderItemRepository, never()).findById(anyLong());
        verify(refundService, never()).createRequestedRefund(
                any(Payment.class),
                anyString(),
                anyLong(),
                anyLong(),
                anyLong(),
                anyLong(),
                anyLong(),
                anyString()
        );
    }

    @Test
    void 부분환불_완료_시_Refund는_COMPLETED_Payment는_PARTIAL_REFUNDED가_된다() {
        // given
        Payment payment = createPaidPayment(1L, 10L, 10_000L, 10_000L, 0L, 0);
        Product product = createProduct(101L, 5_000, 10);
        OrderItem orderItem = createOrderItem(payment.getOrder(), 11L, product.getId(), 5_000, 1);
        Refund refund = Refund.createRequested(payment, "부분 환불", 5_000L, 0L, 5_000L, 0L, 0L, "idem");
        setId(refund, 100L);
        RefundItem refundItem = RefundItem.create(refund, orderItem, 1, 0L, 5_000L);

        when(refundService.findById(100L)).thenReturn(refund);
        when(refundService.findItemsByRefundId(100L)).thenReturn(List.of(refundItem));
        when(productRepository.findByIdWithLock(product.getId())).thenReturn(Optional.of(product));
        when(refundService.calculateCompletedTotalRefundAmount(payment.getId())).thenReturn(5_000L);

        // when
        RefundResponse response = refundCommandService.completeRequestedRefund(100L);

        // then
        assertThat(response.refundStatus()).isEqualTo("COMPLETED");
        assertThat(response.paymentStatus()).isEqualTo("PARTIAL_REFUNDED");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PARTIAL_REFUNDED);
        assertThat(product.getStockQuantity()).isEqualTo(11);
    }

    @Test
    void 전액환불_완료_시_Payment는_REFUNDED_Order는_CANCELED가_된다() {
        // given
        Payment payment = createPaidPayment(1L, 10L, 10_000L, 10_000L, 0L, 0);
        Product product = createProduct(101L, 10_000, 10);
        OrderItem orderItem = createOrderItem(payment.getOrder(), 11L, product.getId(), 10_000, 1);
        Refund refund = Refund.createRequested(payment, "전액 환불", 10_000L, 0L, 10_000L, 0L, 0L, "idem");
        setId(refund, 100L);
        RefundItem refundItem = RefundItem.create(refund, orderItem, 1, 0L, 10_000L);

        when(refundService.findById(100L)).thenReturn(refund);
        when(refundService.findItemsByRefundId(100L)).thenReturn(List.of(refundItem));
        when(productRepository.findByIdWithLock(product.getId())).thenReturn(Optional.of(product));
        when(refundService.calculateCompletedTotalRefundAmount(payment.getId())).thenReturn(10_000L);

        // when
        RefundResponse response = refundCommandService.completeRequestedRefund(100L);

        // then
        assertThat(response.refundStatus()).isEqualTo("COMPLETED");
        assertThat(response.paymentStatus()).isEqualTo("REFUNDED");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getOrder().getStatus().name()).isEqualTo("CANCELED");
    }

    private Payment createPaidPayment(
            Long memberId,
            Long orderId,
            Long totalAmount,
            Long pgAmount,
            Long earnedPointAmount,
            int memberPoint
    ) {
        Member member = new Member(
                "member" + memberId + "@test.com",
                "password",
                "테스터",
                "010-1234-5678",
                memberPoint
        );
        setId(member, memberId);

        Order order = Order.createPending(member, "ORD-TEST-" + orderId, totalAmount.intValue(), 0);
        setId(order, orderId);
        order.completePayment(0);

        Payment payment = Payment.createReady(order, "pay_" + orderId, totalAmount, pgAmount, earnedPointAmount);
        setId(payment, orderId);
        payment.complete(LocalDateTime.now());
        return payment;
    }

    private OrderItem createOrderItem(Order order, Long orderItemId, Long productId, int price, int quantity) {
        Product product = createProduct(productId, price, 10);
        OrderItem orderItem = OrderItem.createProductSnapshot(order, product, quantity);
        setId(orderItem, orderItemId);
        return orderItem;
    }

    private Product createProduct(Long productId, int price, int stockQuantity) {
        Product product = Product.create("테스트 상품", price, stockQuantity, FOOD, FOR_SALE);
        setId(product, productId);
        return product;
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
