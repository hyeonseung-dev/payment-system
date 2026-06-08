# API 명세

커머스 결제 시스템의 실제 구현 Controller 기준 API 명세입니다.

## 공통 사항

### Base URL

```text
http://localhost:8080
```

### 공통 성공 응답 형식

대부분의 Controller는 `ApiResponse<T>`로 응답합니다.

```json
{
  "code": "SUCCESS",
  "message": null,
  "data": {}
}
```

`data`가 없는 성공 응답은 다음 형식입니다.

```json
{
  "code": "SUCCESS"
}
```

메시지만 있는 성공 응답은 다음 형식입니다.

```json
{
  "code": "SUCCESS",
  "message": "회원가입이 완료되었습니다."
}
```

### 공통 실패 응답 형식

`BusinessException`, DTO 검증 실패, JSON 파싱 실패는 다음 형식으로 응답합니다.

```json
{
  "code": "COMMON_001",
  "message": "입력값이 올바르지 않습니다."
}
```

### 인증

JWT 인증이 필요한 API는 다음 헤더가 필요합니다.

```http
Authorization: Bearer {accessToken}
```

회원가입, 로그인, 상품 조회, PortOne 공개 설정 조회, 웹훅을 제외한 장바구니/주문/포인트/결제/환불 API는 JWT 인증이 필요합니다.

### 주요 ErrorCode

| 도메인 | code | message |
| --- | --- | --- |
| 공통 | `COMMON_001` | 입력값이 올바르지 않습니다. |
| 공통 | `COMMON_002` | 서버 내부 오류가 발생했습니다. |
| 인증 | `AUTH_001` | 인증이 필요합니다. |
| 인증 | `AUTH_002` | 유효하지 않은 토큰입니다. |
| 인증 | `AUTH_003` | 만료된 토큰입니다. |
| 인증 | `AUTH_004` | 접근 권한이 없습니다. |
| 회원 | `MEMBER_001` | 회원을 찾을 수 없습니다. |
| 회원 | `MEMBER_002` | 이미 존재하는 이메일입니다. |
| 회원 | `MEMBER_003` | 이메일 또는 비밀번호가 올바르지 않습니다. |
| 회원 | `MEMBER_004` | 이미 존재하는 전화번호입니다. |
| 상품 | `PRODUCT_001` | 상품을 찾을 수 없습니다. |
| 상품 | `PRODUCT_002` | 재고가 부족합니다. |
| 장바구니 | `CART_002` | 장바구니 상품을 찾을 수 없습니다. |
| 장바구니 | `CART_005` | 요청 수량이 재고를 초과합니다. |
| 장바구니 | `CART_006` | 장바구니가 존재하지 않습니다. |
| 주문 | `ORDER_001` | 주문을 찾을 수 없습니다. |
| 주문 | `ORDER_003` | 주문 상품을 찾을 수 없습니다. |
| 포인트 | `POINT_002` | 포인트 잔액이 부족합니다. |
| 포인트 | `POINT_004` | 사용할 수 없는 포인트 금액입니다. |
| 결제 | `PAYMENT_001` | 결제 정보를 찾을 수 없습니다. |
| 결제 | `PAYMENT_002` | 결제 금액이 일치하지 않습니다. |
| 결제 | `PAYMENT_003` | 유효하지 않은 결제 상태입니다. |
| 결제 | `PAYMENT_004` | PG사 결제가 완료되지 않았습니다. |
| 결제 | `PAYMENT_006` | 주문과 결제 식별자가 일치하지 않습니다. |
| 환불 | `REFUND_003` | 잔여 환불 가능 수량을 초과했습니다. |
| 환불 | `REFUND_004` | 환불 수량은 1 이상이어야 합니다. |
| 웹훅 | `WEBHOOK_002` | portonePaymentId를 가져올 수 없습니다. |

---

# 인증 / 회원

## 1. 회원가입

### 회원가입

* **Method:** `POST`
* **URL:** `/api/auth/signup`
* **Auth:** 불필요

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| email | String | Y | 이메일 |
| password | String | Y | 6자 이상, 영문자와 숫자 포함 |
| name | String | Y | 이름 |
| phoneNumber | String | Y | 전화번호, `000-0000-0000` 형식 |

```json
{
  "email": "user@example.com",
  "password": "test1234",
  "name": "홍길동",
  "phoneNumber": "010-1234-5678"
}
```

### Response

```json
{
  "code": "SUCCESS",
  "message": "회원가입이 완료되었습니다."
}
```

### 상태코드

`201 Created`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 400 | 요청값 검증 실패 | DTO 검증 메시지 |
| 409 | 이메일 중복 | 이미 존재하는 이메일입니다. |
| 409 | 전화번호 중복 | 이미 존재하는 전화번호입니다. |

---

## 2. 로그인

### 로그인

* **Method:** `POST`
* **URL:** `/api/auth/login`
* **Auth:** 불필요

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| email | String | Y | 이메일 |
| password | String | Y | 6자 이상, 영문자와 숫자 포함 |

```json
{
  "email": "user@example.com",
  "password": "test1234"
}
```

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.test.token",
    "tokenType": "Bearer"
  }
}
```

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 400 | 요청값 검증 실패 | DTO 검증 메시지 |
| 401 | 이메일 또는 비밀번호 불일치 | 이메일 또는 비밀번호가 올바르지 않습니다. |

---

## 3. 로그아웃

### 로그아웃

* **Method:** `POST`
* **URL:** `/api/auth/logout`
* **Auth:** JWT 필요

### Headers

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| Authorization | Y | `Bearer {accessToken}` |

### Response

```json
{
  "code": "SUCCESS",
  "message": "로그아웃이 완료되었습니다."
}
```

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 401 | 인증 실패 | 로그인이 필요한 서비스입니다. |

---

# 상품

## 1. 상품 목록 조회

### 상품 목록 조회

* **Method:** `GET`
* **URL:** `/api/products`
* **Auth:** 불필요

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "content": [
      {
        "productId": 1,
        "name": "테스트 상품",
        "price": 10000,
        "stockQuantity": 50,
        "category": "ELECTRONICS",
        "status": "FOR_SALE"
      }
    ],
    "page": 0,
    "size": 1,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 상태코드

`200 OK`

---

## 2. 상품 상세 조회

### 상품 상세 조회

* **Method:** `GET`
* **URL:** `/api/products/{productId}`
* **Auth:** 불필요

### Path Variable

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| productId | Long | 상품 ID |

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "productId": 1,
    "name": "테스트 상품",
    "price": 10000,
    "stockQuantity": 50,
    "description": "테스트 상품 설명",
    "category": "ELECTRONICS",
    "status": "FOR_SALE"
  }
}
```

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 404 | 존재하지 않는 상품 | 상품을 찾을 수 없습니다. |

---

# 장바구니

## 1. 장바구니 조회

### 장바구니 조회

* **Method:** `GET`
* **URL:** `/api/carts`
* **Auth:** JWT 필요

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "cartId": 1,
    "items": [
      {
        "id": 10,
        "productId": 1,
        "productName": "테스트 상품",
        "price": 10000,
        "quantity": 2,
        "subtotal": 20000
      }
    ],
    "totalAmount": 20000
  }
}
```

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 404 | 장바구니 없음 | 장바구니가 존재하지 않습니다. |

---

## 2. 장바구니 상품 추가

### 장바구니 상품 추가

* **Method:** `POST`
* **URL:** `/api/carts/items`
* **Auth:** JWT 필요

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| productId | Long | Y | 상품 ID |
| quantity | int | Y | 수량, 1 이상 |

```json
{
  "productId": 1,
  "quantity": 2
}
```

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "cartItemId": 10,
    "productId": 1,
    "quantity": 2
  }
}
```

### 상태코드

`201 Created`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 400 | 수량 검증 실패 | 수량은 1이상이여야 합니다. |
| 404 | 상품 없음 | 상품을 찾을 수 없습니다. |
| 409 | 재고 초과 | 요청 수량이 재고를 초과합니다. |

---

## 3. 장바구니 상품 수량 변경

### 장바구니 상품 수량 변경

* **Method:** `PATCH`
* **URL:** `/api/carts/items/{cartItemId}`
* **Auth:** JWT 필요

### Path Variable

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| cartItemId | Long | 장바구니 상품 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| quantity | int | Y | 변경 수량, 1 이상 |

```json
{
  "quantity": 3
}
```

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "cartItemId": 10,
    "quantity": 3
  }
}
```

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 400 | 수량 검증 실패 | 수량은 1이상이어야 합니다. |
| 404 | 장바구니 상품 없음 | 장바구니 상품을 찾을 수 없습니다. |
| 409 | 재고 초과 | 요청 수량이 재고를 초과합니다. |

---

## 4. 장바구니 상품 삭제

### 장바구니 상품 삭제

* **Method:** `DELETE`
* **URL:** `/api/carts/items/{cartItemId}`
* **Auth:** JWT 필요

### Path Variable

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| cartItemId | Long | 장바구니 상품 ID |

### Response

```json
{
  "code": "SUCCESS"
}
```

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 404 | 장바구니 상품 없음 | 장바구니 상품을 찾을 수 없습니다. |

---

# 주문

## 1. 주문서 미리보기

### 주문서 미리보기

* **Method:** `POST`
* **URL:** `/api/orders/preview`
* **Auth:** JWT 필요

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| cartItemIds | List<Long> | Y | 주문할 장바구니 상품 ID 목록 |

```json
{
  "cartItemIds": [10, 11]
}
```

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "items": [
      {
        "cartItemId": 10,
        "productId": 1,
        "productName": "테스트 상품",
        "price": 10000,
        "quantity": 2,
        "subtotal": 20000
      }
    ],
    "totalAmount": 20000
  }
}
```

### 상태코드

`201 Created`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 400 | 장바구니 상품 미선택 | 주문할 장바구니 상품을 선택해주세요 |
| 404 | 장바구니 상품 없음 | 장바구니 상품을 찾을 수 없습니다. |

---

## 2. 장바구니 기반 주문 생성

### 장바구니 기반 주문 생성

* **Method:** `POST`
* **URL:** `/api/orders`
* **Auth:** JWT 필요

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| cartItemIds | List<Long> | Y | 주문할 장바구니 상품 ID 목록 |
| usePointAmount | Integer | N | 사용할 포인트, 미전달 시 0 |

```json
{
  "cartItemIds": [10, 11],
  "usePointAmount": 1000
}
```

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-20260608-000001",
    "portonePaymentId": "pay_ab12cd34",
    "orderStatus": "PAYMENT_PENDING",
    "totalAmount": 20000,
    "pointAmount": 1000,
    "pgAmount": 19000
  }
}
```

### 상태코드

`201 Created`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 400 | 요청값 검증 실패 | DTO 검증 메시지 |
| 400 | 포인트 금액 오류 | 사용할 수 없는 포인트 금액입니다. |
| 401 | 인증 정보 없음 | 인증이 필요합니다. |
| 404 | 장바구니 상품 없음 | 장바구니 상품을 찾을 수 없습니다. |
| 404 | 회원 없음 | 회원을 찾을 수 없습니다. |
| 409 | 재고 부족 | 재고가 부족합니다. |

---

## 3. 상품 바로 주문 생성

### 상품 바로 주문 생성

* **Method:** `POST`
* **URL:** `/api/orders/products`
* **Auth:** JWT 필요

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| productId | Long | Y | 상품 ID |
| quantity | int | Y | 수량, 1 이상 |
| pointAmount | Integer | N | 사용할 포인트, 미전달 시 0 |

```json
{
  "productId": 1,
  "quantity": 2,
  "pointAmount": 1000
}
```

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-20260608-000001",
    "portonePaymentId": "pay_ab12cd34",
    "orderStatus": "PAYMENT_PENDING",
    "totalAmount": 20000,
    "pointAmount": 1000,
    "pgAmount": 19000
  }
}
```

### 상태코드

`201 Created`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 400 | 요청값 검증 실패 | DTO 검증 메시지 |
| 400 | 포인트 금액 오류 | 사용할 수 없는 포인트 금액입니다. |
| 401 | 인증 정보 없음 | 인증이 필요합니다. |
| 404 | 상품 없음 | 상품을 찾을 수 없습니다. |
| 404 | 회원 없음 | 회원을 찾을 수 없습니다. |
| 409 | 재고 부족 | 재고가 부족합니다. |

---

## 4. 내 주문 목록 조회

### 내 주문 목록 조회

* **Method:** `GET`
* **URL:** `/api/orders`
* **Auth:** JWT 필요

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| page | int | N | 0 | 페이지 번호 |
| size | int | N | 10 | 페이지 크기 |

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "content": [
      {
        "orderId": 1,
        "orderNumber": "ORD-20260608-000001",
        "status": "PAYMENT_PENDING",
        "itemCount": 2,
        "totalAmount": 20000,
        "createAt": "2026-06-08T12:30:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 400 | page 또는 size 오류 | 입력값이 올바르지 않습니다. |
| 401 | 인증 정보 없음 | 인증이 필요합니다. |

---

## 5. 주문 상세 조회

### 주문 상세 조회

* **Method:** `GET`
* **URL:** `/api/orders/{orderId}`
* **Auth:** JWT 필요

### Path Variable

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| orderId | Long | 주문 ID |

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-20260608-000001",
    "status": "PAYMENT_PENDING",
    "totalAmount": 20000,
    "pointAmount": 1000,
    "pgAmount": 19000,
    "createAt": "2026-06-08T12:30:00",
    "paymentStatus": "READY",
    "earnedPointAmount": 0,
    "items": [
      {
        "orderItemId": 1,
        "productId": 1,
        "productName": "테스트 상품",
        "price": 10000,
        "quantity": 2,
        "subtotal": 20000
      }
    ]
  }
}
```

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 401 | 인증 정보 없음 | 인증이 필요합니다. |
| 404 | 주문 없음 또는 타인 주문 | 주문을 찾을 수 없습니다. |
| 404 | 결제 정보 없음 | 결제 정보를 찾을 수 없습니다. |

---

# 결제

## 결제 흐름

1. 클라이언트가 장바구니 기반 주문 생성 또는 상품 바로 주문 생성을 호출합니다.
2. 서버는 주문과 `READY` 상태 Payment를 생성하고 `portonePaymentId`를 응답합니다.
3. 클라이언트는 `/api/portone/config`로 PortOne 공개 설정을 조회합니다.
4. 클라이언트는 `portonePaymentId`, `pgAmount` 등을 사용해 PortOne 결제창을 호출합니다.
5. 결제창 완료 후 클라이언트는 `/api/payments/confirm`으로 결제 확정을 요청합니다.
6. 서버는 DB의 `portonePaymentId`와 요청값을 비교하고, PortOne API로 결제 상태와 금액을 재조회합니다.
7. 결제 상태가 `PAID`이고 서버 계산 PG 금액과 PortOne 승인 금액이 일치하면 결제 완료 처리합니다.
8. PortOne 웹훅이 먼저 또는 나중에 도착해도 공통 결제 확정 로직으로 멱등 처리합니다.

## 1. PortOne 공개 설정 조회

### PortOne 공개 설정 조회

* **Method:** `GET`
* **URL:** `/api/portone/config`
* **Auth:** 불필요

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "storeId": "store-test-id",
    "channelKey": "channel-key-test"
  }
}
```

### 상태코드

`200 OK`

---

## 2. 결제 확정

### 결제 확정

* **Method:** `POST`
* **URL:** `/api/payments/confirm`
* **Auth:** JWT 필요

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| orderId | Long | Y | 결제를 확정할 주문 ID |
| portonePaymentId | String | Y | 주문 생성 응답으로 받은 PortOne 결제 식별자 |

```json
{
  "orderId": 1,
  "portonePaymentId": "pay_ab12cd34"
}
```

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "orderId": 1,
    "paymentId": 1,
    "orderStatus": "COMPLETED",
    "paymentStatus": "PAID",
    "totalAmount": 20000,
    "pointAmount": 1000,
    "pgAmount": 19000,
    "earnedPointAmount": 0
  }
}
```

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 400 | 요청값 검증 실패 | DTO 검증 메시지 |
| 400 | PortOne 결제 미완료 | PG사 결제가 완료되지 않았습니다. |
| 400 | 결제 금액 불일치 | 결제 금액이 일치하지 않습니다. |
| 400 | PortOne 결제 ID 불일치 | 주문과 결제 식별자가 일치하지 않습니다. |
| 403 | 타인 주문 결제 확정 | 접근 권한이 없습니다. |
| 404 | 결제 정보 없음 | 결제 정보를 찾을 수 없습니다. |

---

## 3. 결제 전체 취소

### 결제 전체 취소

* **Method:** `POST`
* **URL:** `/api/payments/{paymentId}/cancel`
* **Auth:** JWT 필요

### Path Variable

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| paymentId | Long | 결제 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| reason | String | Y | 결제취소 사유 |

```json
{
  "reason": "고객 요청"
}
```

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "paymentId": 1,
    "orderId": 1,
    "portonePaymentId": "pay_ab12cd34",
    "paymentStatus": "CANCELLED",
    "orderStatus": "CANCELED",
    "message": "결제가 취소되었습니다."
  }
}
```

결제 전체 취소가 완료되면 주문 상태는 `CANCELED`, 결제 상태는 `CANCELLED`로 변경되며 재고 복구, 사용 포인트 복구, 적립 포인트 회수 이력이 함께 처리됩니다.

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 400 | 요청값 검증 실패 | 결제취소 사유는 필수입니다. |
| 400 | 취소 불가능 상태 | 유효하지 않은 결제 상태입니다. |
| 403 | 타인 결제 취소 | 접근 권한이 없습니다. |
| 404 | 결제 정보 없음 | 결제 정보를 찾을 수 없습니다. |
| 500 | PG 취소 실패 | PG 취소 요청에 실패했습니다. |

---

# 환불

## 환불 흐름

1. 클라이언트는 환불할 `paymentId`, `orderItemId`, 수량을 서버에 전달합니다.
2. 서버는 결제 소유권, 환불 가능 결제 상태, 주문 상품 소속, 중복 상품, 잔여 환불 가능 수량을 검증합니다.
3. 환불 금액은 클라이언트 값을 사용하지 않고 주문 상품 스냅샷 가격과 수량으로 서버가 계산합니다.
4. 서버는 PortOne 환불 요청 전에 `REQUESTED` 환불을 저장해 수량을 선점합니다.
5. PG 환불액이 있으면 PortOne 취소 API를 호출합니다.
6. PortOne 환불 성공 후 재고 복구, 포인트 복구/회수, 결제 상태 변경을 처리합니다.

## 1. 환불 요청

### 환불 요청

* **Method:** `POST`
* **URL:** `/api/payments/{paymentId}/refunds`
* **Auth:** JWT 필요

### Path Variable

| 파라미터 | 타입 | 설명 |
| --- | --- | --- |
| paymentId | Long | 결제 ID |

### Request Body

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| reason | String | Y | 환불 사유 |
| items | List<RefundItemRequest> | Y | 환불할 주문 상품 목록 |
| items[].orderItemId | Long | Y | 환불할 주문 상품 ID |
| items[].quantity | Integer | Y | 환불 수량, 1 이상 |

```json
{
  "reason": "단순 변심",
  "items": [
    {
      "orderItemId": 1,
      "quantity": 1
    }
  ]
}
```

### Response

```json
{
  "code": "SUCCESS",
  "data": {
    "refundId": 1,
    "paymentId": 1,
    "refundStatus": "COMPLETED",
    "totalRefundAmount": 10000,
    "pointRefundAmount": 500,
    "pgRefundAmount": 9500,
    "earnedPointCancelAmount": 0,
    "earnedPointDeductionAmount": 0,
    "paymentStatus": "PARTIAL_REFUNDED"
  }
}
```

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 400 | 요청값 검증 실패 | DTO 검증 메시지 |
| 400 | 환불 불가능 상태 | 환불 가능한 결제 상태가 아닙니다. |
| 400 | 환불 수량 오류 | 환불 수량은 1 이상이어야 합니다. |
| 400 | 같은 주문 상품 중복 요청 | 입력값이 올바르지 않습니다. |
| 403 | 타인 결제 또는 다른 주문 상품 환불 | 접근 권한이 없습니다. |
| 404 | 결제 정보 없음 | 결제 정보를 찾을 수 없습니다. |
| 404 | 주문 상품 없음 | 주문 상품을 찾을 수 없습니다. |
| 409 | 환불 가능 수량 초과 | 잔여 환불 가능 수량을 초과했습니다. |

---

# 포인트

## 1. 포인트 잔액 조회

### 포인트 잔액 조회

* **Method:** `GET`
* **URL:** `/api/points/balance`
* **Auth:** JWT 필요

### Response

```json
{
  "memberId": 1,
  "pointBalance": 5000
}
```

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 404 | 회원 없음 | 회원을 찾을 수 없습니다. |

---

## 2. 포인트 거래 내역 조회

### 포인트 거래 내역 조회

* **Method:** `GET`
* **URL:** `/api/points/histories`
* **Auth:** JWT 필요

### Query Parameters

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| page | int | N | 0 | 페이지 번호 |
| size | int | N | 10 | 페이지 크기 |

### Response

```json
{
  "content": [
    {
      "pointHistoryId": 1,
      "paymentId": 1,
      "type": "USE",
      "amount": 1000,
      "balanceAfter": 4000,
      "createdAt": "2026-06-08T12:30:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1
}
```

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 400 | page 또는 size 오류 | 입력값이 올바르지 않습니다. |
| 404 | 회원 없음 | 회원을 찾을 수 없습니다. |

---

# 웹훅

## PortOne 웹훅 처리 흐름

1. PortOne이 `/api/webhooks/portone`으로 웹훅을 전송합니다.
2. 서버는 `webhook-id`, `webhook-timestamp`, `webhook-signature` 헤더와 원문 body로 서명을 검증합니다.
3. 서명 검증 실패 시 결제 도메인 로직을 호출하지 않고 `200 OK`로 종료합니다.
4. 서명 검증 성공 시 웹훅 본문에서 `portonePaymentId`를 추출합니다.
5. `Transaction.Paid` 웹훅은 PortOne API 재조회 후 결제 확정 공통 로직을 호출합니다.
6. `Transaction.Cancelled` 웹훅은 PortOne API 재조회 후 결제취소 동기화 로직을 호출합니다.
7. 이미 `COMPLETED` 또는 `IGNORED` 상태로 저장된 웹훅은 중복 처리하지 않습니다.

## 1. PortOne 웹훅 수신

### PortOne 웹훅 수신

* **Method:** `POST`
* **URL:** `/api/webhooks/portone`
* **Auth:** JWT 불필요, PortOne 웹훅 서명 검증 필요

### Headers

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| webhook-id | Y | PortOne 웹훅 식별자 |
| webhook-timestamp | Y | PortOne 웹훅 생성 시각 |
| webhook-signature | Y | PortOne 웹훅 서명 |

### Request Body

PortOne Standard Webhooks 원문 JSON을 그대로 전달합니다. 서버는 본문을 직접 신뢰하지 않고 서명 검증 후 PortOne API로 결제 정보를 재조회합니다.

```json
{
  "type": "Transaction.Paid",
  "data": {
    "paymentId": "pay_ab12cd34"
  }
}
```

### Response

```json
{
  "code": "SUCCESS"
}
```

### 상태코드

`200 OK`

### Error

| 상태코드 | 상황 | message |
| --- | --- | --- |
| 200 | 서명 검증 실패 | 성공 응답 후 처리 중단 |
| 400 | portonePaymentId 추출 실패 | portonePaymentId를 가져올 수 없습니다. |

---

# 참고 Enum

| 구분 | 값 |
| --- | --- |
| ProductCategory | `CLOTHES`, `FOOD`, `ELECTRONICS` |
| ProductStatus | `FOR_SALE`, `SOLD_OUT`, `DISCONTINUED` |
| OrderStatus | `PAYMENT_PENDING`, `COMPLETED`, `CANCELED` |
| PaymentStatus | `READY`, `PAID`, `FAILED`, `CANCELLED`, `PARTIAL_REFUNDED`, `REFUNDED` |
| RefundStatus | `REQUESTED`, `COMPLETED`, `FAILED` |
| PointHistoryType | `USE`, `EARN`, `USE_CANCEL`, `EARN_CANCEL` |
