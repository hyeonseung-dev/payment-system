package com.example.paymentsystem.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        String name,
        String phoneNumber
) {
    public static LoginResponse of(String accessToken, String name, String phoneNumber) {
        return new LoginResponse(accessToken, "Bearer", name, phoneNumber);
    }
}