package com.example.paymentsystem.domain.refund.controller;

import com.example.paymentsystem.domain.refund.dto.RefundRequest;
import com.example.paymentsystem.domain.refund.dto.RefundResponse;
import com.example.paymentsystem.domain.refund.facade.RefundFacade;
import com.example.paymentsystem.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 환불 API 요청을 처리하는 컨트롤러이다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments/{paymentId}/refunds")
public class RefundController {

    private final RefundFacade refundFacade;

    /**
     * 결제에 포함된 주문 상품을 환불한다.
     *
     * @param memberId 인증 회원 ID
     * @param paymentId 결제 ID
     * @param request 환불 요청
     * @return 환불 응답
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RefundResponse>> refundPayment(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long paymentId,
            @Valid @RequestBody RefundRequest request
    ) {
        RefundResponse response = refundFacade.refundPayment(
                memberId,
                paymentId,
                request.reason(),
                request.items()
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
