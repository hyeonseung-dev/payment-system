package com.example.paymentsystem.domain.payment.service;

import com.example.paymentsystem.domain.order.entity.Order;
import com.example.paymentsystem.domain.payment.entity.Payment;
import com.example.paymentsystem.domain.payment.repository.PaymentRepository;
import com.example.paymentsystem.global.error.BusinessException;
import com.example.paymentsystem.global.error.ErrorCode;
import com.example.paymentsystem.infra.portone.client.PortOnePaymentIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 결제 생성과 조회를 담당하는 서비스이다.
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * 결제 대기 상태의 Payment를 생성한다.
     *
     * <p>주문 생성 흐름에서 호출하며, 서버가 생성한 PortOne 결제 식별자를 Payment에 저장한다.</p>
     *
     * @param order 주문
     * @param totalAmount 주문 총 금액
     * @param pgAmount PG 결제 금액
     * @param earnedPointAmount 적립 예정 포인트
     * @return 저장된 결제 정보
     */
    @Transactional
    public Payment createReadyPayment(
            Order order,
            Long totalAmount,
            Long pgAmount,
            Long earnedPointAmount
    ) {
        String portonePaymentId = PortOnePaymentIdGenerator.generatePortonePaymentId();
        Payment payment = Payment.createReady(
                order,
                portonePaymentId,
                totalAmount,
                pgAmount,
                earnedPointAmount
        );
        return paymentRepository.save(payment);
    }

    /**
     * 주문 ID로 주문 정보와 함께 Payment를 조회한다.
     *
     * @param orderId 주문 ID
     * @return 주문 정보가 포함된 결제 정보
     */
    @Transactional(readOnly = true)
    public Payment findByOrderIdWithOrder(Long orderId) {
        return paymentRepository.findByOrderIdWithOrder(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    /**
     * 결제 ID로 주문과 회원 정보가 포함된 Payment를 조회한다.
     *
     * @param paymentId 결제 ID
     * @return 주문과 회원 정보가 포함된 결제 정보
     */
    @Transactional(readOnly = true)
    public Payment findByIdWithOrderAndMember(Long paymentId) {
        return paymentRepository.findByIdWithOrderAndMember(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    /**
     * PortOne 결제 식별자로 주문과 회원 정보가 포함된 Payment를 조회한다.
     *
     * @param portonePaymentId PortOne 결제 식별자
     * @return 주문과 회원 정보가 포함된 결제 정보
     */
    @Transactional(readOnly = true)
    public Payment findByPortonePaymentIdWithOrderAndMember(String portonePaymentId) {
        return paymentRepository.findByPortonePaymentIdWithOrderAndMember(portonePaymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }

    /**
     * PortOne 결제 식별자로 주문과 회원 정보가 포함된 Payment를 조회한다.
     *
     * <p>웹훅 이력은 Payment를 찾지 못한 경우에도 남길 수 있어야 하므로 Optional로 반환한다.</p>
     *
     * @param portonePaymentId PortOne 결제 식별자
     * @return 주문과 회원 정보가 포함된 결제 정보
     */
    @Transactional(readOnly = true)
    public Optional<Payment> findOptionalByPortonePaymentIdWithOrderAndMember(String portonePaymentId) {
        return paymentRepository.findByPortonePaymentIdWithOrderAndMember(portonePaymentId);
    }

    /**
     * Payment를 결제 완료 상태로 변경한다.
     *
     * @param payment 결제 정보
     * @param paidAt 결제 완료 시각
     */
    @Transactional
    public void confirmPayment(Payment payment, LocalDateTime paidAt) {
        Payment managedPayment = findById(payment.getId());
        managedPayment.complete(paidAt);
    }

    /**
     * Payment를 결제 실패 상태로 변경한다.
     *
     * @param payment 결제 정보
     */
    @Transactional
    public void failPayment(Payment payment) {
        Payment managedPayment = findById(payment.getId());
        managedPayment.fail();
    }

    /**
     * Payment를 취소 상태로 변경한다.
     *
     * @param payment 결제 정보
     */
    @Transactional
    public void cancelPayment(Payment payment) {
        Payment managedPayment = findById(payment.getId());
        managedPayment.cancel();
    }

    private Payment findById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND));
    }
}
