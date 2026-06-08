package com.example.paymentsystem.domain.refund.repository;

import com.example.paymentsystem.domain.refund.entity.Refund;
import com.example.paymentsystem.domain.refund.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 결제 ID와 환불 상태로 총 환불 금액 합계를 조회한다.
     *
     * @param paymentId 결제 ID
     * @param status 환불 상태
     * @return 총 환불 금액 합계
     */
    @Query("""
            select coalesce(sum(r.totalRefundAmount), 0)
            from Refund r
            where r.payment.id = :paymentId
              and r.status = :status
            """)
    Long sumTotalRefundAmountByPaymentIdAndStatus(
            @Param("paymentId") Long paymentId,
            @Param("status") RefundStatus status
    );

    /**
     * 결제 ID와 환불 상태로 PG 환불 금액 합계를 조회한다.
     *
     * @param paymentId 결제 ID
     * @param status 환불 상태
     * @return PG 환불 금액 합계
     */
    @Query("""
            select coalesce(sum(r.pgRefundAmount), 0)
            from Refund r
            where r.payment.id = :paymentId
              and r.status = :status
            """)
    Long sumPgRefundAmountByPaymentIdAndStatus(
            @Param("paymentId") Long paymentId,
            @Param("status") RefundStatus status
    );
}
