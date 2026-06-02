# docs/API_SPEC.md

# API Spec

## Auth

| 기능 | Method | URL | 인증 |
|---|---|---|---|
| 회원가입 | POST | `/api/auth/signup` | X |
| 로그인 | POST | `/api/auth/login` | X |

## Product

| 기능 | Method | URL | 인증 |
|---|---|---|---|
| 상품 목록 조회 | GET | `/api/products` | X |
| 상품 단건 조회 | GET | `/api/products/{productId}` | X |

## Cart

| 기능 | Method | URL | 인증 |
|---|---|---|---|
| 장바구니 담기 | POST | `/api/carts/items` | O |
| 장바구니 조회 | GET | `/api/carts` | O |
| 수량 변경 | PATCH | `/api/carts/items/{cartItemId}` | O |
| 상품 삭제 | DELETE | `/api/carts/items/{cartItemId}` | O |

## Order

| 기능 | Method | URL | 인증 |
|---|---|---|---|
| 주문서 미리보기 | POST | `/api/carts/orders/preview` | O |
| 주문 생성 | POST | `/api/carts/orders` | O |
| 내 주문 목록 조회 | GET | `/api/orders` | O |
| 주문 상세 조회 | GET | `/api/orders/{orderId}` | O |
| 결제대기 주문 취소 | POST | `/api/orders/{orderId}/cancel` | O |

## Point

| 기능 | Method | URL | 인증 |
|---|---|---|---|
| 포인트 잔액 조회 | GET | `/api/points/balance` | O |
| 포인트 거래 내역 조회 | GET | `/api/points/histories` | O |

## Payment

| 기능 | Method | URL | 인증 |
|---|---|---|---|
| 결제 확정 | POST | `/api/payments/confirm` | O |

PaymentConfirmRequest:

```json
{
  "orderId": 1,
  "portonePaymentId": "payment-xxx"
}
````

## Refund

| 기능    | Method | URL                                 | 인증 |
| ----- | ------ | ----------------------------------- | -- |
| 환불 요청 | POST   | `/api/payments/{paymentId}/refunds` | O  |

## Webhook

| 기능            | Method | URL                     | 인증 |
| ------------- | ------ | ----------------------- | -- |
| PortOne 웹훅 수신 | POST   | `/api/webhooks/portone` | X  |
