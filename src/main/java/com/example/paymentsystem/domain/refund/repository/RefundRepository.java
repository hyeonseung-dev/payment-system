package com.example.paymentsystem.domain.refund.repository;

import com.example.paymentsystem.domain.refund.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 환불 엔티티의 영속성 처리를 담당한다.
 */
public interface RefundRepository extends JpaRepository<Refund, Long> {

    /**
     * 결제 ID로 환불 목록을 조회한다.
     *
     * @param paymentId 결제 ID
     * @return 해당 결제의 환불 목록
     */
    List<Refund> findByPaymentId(Long paymentId);
}
