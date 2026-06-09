package com.example.paymentsystem.domain.refund.entity;

/**
 * 환불 처리 상태를 나타낸다.
 *
 * <p>Refund는 환불 요청 1건의 처리 결과를 저장한다.
 * 환불 처리는 완료 또는 실패 상태로 기록한다.</p>
 *
 * <ul>
 *     <li>REQUESTED: 환불 요청이 저장되어 수량을 선점한 상태</li>
 *     <li>COMPLETED: 환불이 정상적으로 완료된 상태</li>
 *     <li>FAILED: PG 환불 실패 등으로 환불이 완료되지 못한 상태</li>
 * </ul>
 */
public enum RefundStatus {

    REQUESTED,
    COMPLETED,
    FAILED
}
