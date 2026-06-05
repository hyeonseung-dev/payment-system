package com.example.paymentsystem.domain.payment.controller;

import com.example.paymentsystem.domain.payment.dto.PaymentConfirmRequest;
import com.example.paymentsystem.domain.payment.dto.PaymentConfirmResponse;
import com.example.paymentsystem.domain.payment.facade.PaymentFacade;
import com.example.paymentsystem.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 API 요청을 처리하는 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentFacade paymentFacade;

    /**
     * 결제를 확정한다.
     *
     * @param memberId 인증 회원 ID
     * @param request 결제 확정 요청
     * @return 결제 확정 응답
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentConfirmResponse>> confirmPayment(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody PaymentConfirmRequest request
    ) {
        PaymentConfirmResponse response = paymentFacade.confirmPayment(
                memberId,
                request.orderId(),
                request.portonePaymentId()
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
