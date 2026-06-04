package com.example.paymentsystem.domain.auth.controller;

import com.example.paymentsystem.domain.auth.dto.LoginRequest;
import com.example.paymentsystem.domain.auth.dto.LoginResponse;
import com.example.paymentsystem.domain.auth.dto.SignupRequest;
import com.example.paymentsystem.domain.auth.service.AuthService;
import com.example.paymentsystem.global.response.ApiResponse;
import com.example.paymentsystem.global.security.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/signup")
    public ResponseEntity<ApiResponse<Void>> signup(
            @Valid @RequestBody SignupRequest request
            ){
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("회원가입이 완료되었습니다."));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity
                .ok(ApiResponse.ok(response));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request
    ) {
        // Authorization 헤더에서 토큰 추출
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) &&
                bearerToken.startsWith(JwtUtil.BEARER_PREFIX)) {
            String token = bearerToken.substring(JwtUtil.BEARER_PREFIX.length());
            authService.logout(token);
        }

        return ResponseEntity.ok(ApiResponse.ok("로그아웃이 완료되었습니다."));
    }
}
