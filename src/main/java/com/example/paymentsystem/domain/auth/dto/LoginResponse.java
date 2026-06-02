package com.example.paymentsystem.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType
) {
    public static LoginResponse from(String accessToken) {
        return new LoginResponse(accessToken, "Bearer");
    }
}