package com.example.paymentsystem.infra.portone.controller;

import com.example.paymentsystem.infra.portone.config.PortOneProperties;
import com.example.paymentsystem.infra.portone.dto.PortOneConfigResponse;
import com.example.paymentsystem.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 클라이언트 결제창 호출에 필요한 PortOne 설정을 제공하는 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portone")
public class PortOneConfigController {

    private final PortOneProperties portOneProperties;

    /**
     * 클라이언트 결제창 호출에 필요한 PortOne 공개 설정을 조회한다.
     *
     * @return PortOne 공개 설정 응답
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<PortOneConfigResponse>> getPortOneConfig() {
        PortOneConfigResponse response = new PortOneConfigResponse(
                portOneProperties.storeId(),
                portOneProperties.channelKey()
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
