package com.example.paymentsystem.infra.portone.dto;

/**
 * 클라이언트 결제창 호출에 필요한 PortOne 설정 응답 DTO이다.
 *
 * @param storeId PortOne 상점 식별자
 * @param channelKey PortOne 채널 키
 */
public record PortOneConfigResponse(
        String storeId,
        String channelKey
) {
}
