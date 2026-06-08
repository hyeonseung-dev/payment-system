package com.example.paymentsystem.domain.cart.repository;

import com.example.paymentsystem.domain.cart.entity.CartItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    @Query("SELECT ci FROM CartItem ci join fetch ci.product where ci.cart.member.id = :memberId")
    List<CartItem> findAllByMemberId(@Param("memberId") Long memberId);

    Optional<CartItem> findByCart_Member_IdAndProduct_Id(Long memberId, Long productId);

    @Modifying
    @Query("DELETE FROM CartItem ci" +
            "    WHERE ci.id = :cartItemId" +
            "      AND ci.cart.id IN (" +
            "          SELECT c.id" +
            "          FROM Cart c" +
            "          WHERE c.member.id = :memberId" +
            "      )")
    int deleteByIdAndCart_Member_Id(@Param("cartItemId") Long cartItemId, @Param("memberId") Long memberId);

    /**
     * 장바구니 상품 ID 목록에 해당하는 항목을 삭제한다.
     *
     * @param cartItemIds 장바구니 상품 ID 목록
     * @return 삭제된 행 수
     */
    @Modifying
    @Query("""
            delete from CartItem ci
            where ci.id in :cartItemIds
            """)
    int deleteByIdIn(@Param("cartItemIds") List<Long> cartItemIds);

    Optional<CartItem> findByIdAndCart_Member_Id(Long itemId, Long memberId);

    @Query("SELECT ci FROM CartItem ci join FETCH ci.product join FETCH ci.cart c WHERE ci.id IN :cartItemIds AND c.member.id = :memberId")
    List<CartItem> findAllByIdsAndMemberId(@Param("cartItemIds") List<Long> cartItemIds, @Param("memberId") Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ci FROM CartItem ci JOIN FETCH ci.product JOIN FETCH ci.cart c WHERE ci.id IN :cartItemIds AND c.member.id = :memberId")
    List<CartItem> findAllByIdsAndMemberIdWithLock(@Param("cartItemIds") List<Long> cartItemIds, @Param("memberId") Long memberId);
}
