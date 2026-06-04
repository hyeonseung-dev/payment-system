package com.example.paymentsystem.domain.payment.entity;

/**
 * 결제 상태 머신을 나타낸다.
 *
 * <p>결제는 주문 생성 직후 READY 상태로 시작하며, 이후 결제 성공/실패/환불 흐름에 따라 상태가 변경된다.
 * 상태 변경은 Payment 엔티티의 도메인 메서드를 통해서만 수행한다.</p>
 *
 * <ul>
 *     <li>READY -> PAID: PG 결제가 성공적으로 검증된 경우</li>
 *     <li>READY -> FAILED: PG 결제 실패, 금액 불일치 등 결제가 완료되지 못한 경우</li>
 *     <li>PAID -> PAID: 결제 완료 중복 호출을 멱등하게 처리하는 경우</li>
 *     <li>PAID -> PARTIAL_REFUNDED: 결제 완료 후 일부 상품만 환불된 경우</li>
 *     <li>PAID -> REFUNDED: 결제 완료 후 전체 금액이 환불된 경우</li>
 *     <li>PARTIAL_REFUNDED -> PARTIAL_REFUNDED: 반복 부분 환불을 처리하는 경우</li>
 *     <li>PARTIAL_REFUNDED -> REFUNDED: 부분 환불 상태에서 남은 금액까지 모두 환불된 경우</li>
 * </ul>
 *
 * <p>FAILED와 REFUNDED는 더 이상 다른 상태로 전이될 수 없는 최종 상태이다.</p>
 */
public enum PaymentStatus {

    READY,
    PAID,
    FAILED,
    PARTIAL_REFUNDED,
    REFUNDED;

    /**
     * 현재 상태에서 다음 상태로 변경할 수 있는지 확인한다.
     *
     * @param nextStatus 변경하려는 다음 결제 상태
     * @return 전이 가능하면 true, 불가능하면 false
     */
    public boolean canTransitTo(PaymentStatus nextStatus) {
        return switch (this) {
            case READY -> nextStatus == PAID || nextStatus == FAILED;
            case PAID -> nextStatus == PAID
                    || nextStatus == PARTIAL_REFUNDED
                    || nextStatus == REFUNDED;
            case PARTIAL_REFUNDED -> nextStatus == PARTIAL_REFUNDED
                    || nextStatus == REFUNDED;
            case FAILED, REFUNDED -> false;
        };
    }
}
