package com.example.paymentsystem.global.response;

import com.example.paymentsystem.global.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private static final String SUCCESS_CODE = "SUCCESS";

    private final String code;
    private final String message;
    private final T data;

    private ApiResponse(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 성공 응답 ────────────────────────────────────────────

    // data 포함 성공
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS_CODE, null, data);
    }

    // data 없는 성공 (삭제, 비우기 등)
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(SUCCESS_CODE, null, null);
    }

    // 실패 응답 ────────────────────────────────────────────

    // ErrorCode 기반 실패
    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    // ErrorCode + 커스텀 메시지 (검증 실패 상세 메시지 등)
    public static ApiResponse<Void> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }

    // code + message 직접 지정 (예상치 못한 예외 fallback)
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
