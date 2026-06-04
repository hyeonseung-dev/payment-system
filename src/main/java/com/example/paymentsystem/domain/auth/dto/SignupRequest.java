package com.example.paymentsystem.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{6,}$",
                message = "비밀번호는 6자 이상, 영문자와 숫자를 포함해야 합니다."
        )
        String password,

        @NotBlank(message = "이름을 입력해주세요.")
        String name,

        @NotBlank(message = "전화번호를 입력해주세요.")
        @Pattern(
                regexp = "^\\d{3}-\\d{3,4}-\\d{4}$",
                message = "전화번호 형식이 올바르지 않습니다."
        )
        String phoneNumber
) {
}
