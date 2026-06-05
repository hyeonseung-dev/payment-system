package com.example.paymentsystem.domain.refund.repository;

import com.example.paymentsystem.domain.refund.entity.RefundItem;
import com.example.paymentsystem.domain.refund.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 환불 상품 엔티티의 영속성 처리를 담당한다.
 */
public interface RefundItemRepository extends JpaRepository<RefundItem, Long> {

    /**
     * 환불 ID로 환불 상품 목록을 조회한다.
     *
     * @param refundId 환불 ID
     * @return 해당 환불의 환불 상품 목록
     */
    List<RefundItem> findByRefundId(Long refundId);

    /**
     * 주문 상품 ID로 환불 상품 목록을 조회한다.
     *
     * @param orderItemId 주문 상품 ID
     * @return 해당 주문 상품의 환불 상품 목록
     */
    List<RefundItem> findByOrderItemId(Long orderItemId);

    /**
     * 주문 상품 ID와 환불 상태로 환불 완료 수량 합계를 조회한다.
     *
     * @param orderItemId 주문 상품 ID
     * @param status 환불 상태
     * @return 환불 수량 합계
     */
    @Query("""
            select coalesce(sum(ri.quantity), 0)
            from RefundItem ri
            where ri.orderItem.id = :orderItemId
              and ri.refund.status = :status
            """)
    Long sumQuantityByOrderItemIdAndRefundStatus(
            @Param("orderItemId") Long orderItemId,
            @Param("status") RefundStatus status
    );
}
