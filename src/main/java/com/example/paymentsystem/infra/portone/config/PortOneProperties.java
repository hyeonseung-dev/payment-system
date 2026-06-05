package com.example.paymentsystem.infra.portone.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * PortOne API 연동에 필요한 설정값을 관리한다.
 *
 * @param baseUrl PortOne API 기본 URL
 * @param apiSecret PortOne API Secret
 * @param storeId PortOne 상점 식별자
 * @param channelKey PortOne 채널 키
 * @param webhookSecret PortOne 웹훅 검증 Secret
 */
@ConfigurationProperties(prefix = "portone")
public record PortOneProperties(
        @DefaultValue("https://api.portone.io")
        String baseUrl,
        String apiSecret,
        String storeId,
        String channelKey,
        String webhookSecret
) {
}
