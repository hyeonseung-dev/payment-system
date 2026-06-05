package com.example.paymentsystem.domain.payment.repository;

import com.example.paymentsystem.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 결제 엔티티의 영속성 처리를 담당한다.
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * 주문 ID로 주문 정보와 함께 결제 정보를 조회한다.
     *
     * @param orderId 주문 ID
     * @return 주문 정보가 포함된 결제 정보
     */
    @Query("""
            select p
            from Payment p
            join fetch p.order o
            join fetch o.member
            where p.order.id = :orderId
            """)
    Optional<Payment> findByOrderIdWithOrder(@Param("orderId") Long orderId);

    /**
     * PortOne 결제 식별자로 결제 정보를 조회한다.
     *
     * @param portonePaymentId PortOne 결제 식별자
     * @return 결제 정보
     */
    Optional<Payment> findByPortonePaymentId(String portonePaymentId);

    /**
     * PortOne 결제 식별자의 존재 여부를 확인한다.
     *
     * @param portonePaymentId PortOne 결제 식별자
     * @return 존재하면 true
     */
    boolean existsByPortonePaymentId(String portonePaymentId);
}
