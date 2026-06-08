package com.example.paymentsystem.infra.portone.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * PortOne API 연동에 필요한 Bean을 설정한다.
 */
@Configuration
@EnableConfigurationProperties(PortOneProperties.class)
public class PortOneConfig {

    private static final int CONNECT_TIMEOUT_MILLISECONDS = 3000;
    private static final int READ_TIMEOUT_MILLISECONDS = 5000;

    /**
     * PortOne API 호출에 사용할 RestClient를 생성한다.
     *
     * @param portOneProperties PortOne 설정값
     * @return PortOne 전용 RestClient
     */
    @Bean
    public RestClient portOneRestClient(PortOneProperties portOneProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLISECONDS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLISECONDS);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(portOneProperties.baseUrl())
                .defaultHeader("Authorization", "PortOne " + portOneProperties.apiSecret())
                .build();
    }
}
