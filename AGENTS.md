# AGENTS.md

## Project

커머스 결제 시스템 팀 프로젝트

## Tech Stack

* Java 17
* Spring Boot
* Spring Data JPA
* MySQL
* JWT
* PortOne V2
* Gradle

## Scope

필수 기능만 구현한다.

구현 범위:

* 회원가입 / 로그인
* JWT 인증
* 상품 조회
* 장바구니
* 주문
* 포인트
* 결제
* 환불
* 웹훅

구현 제외:

* 구독
* 멤버십 등급
* 관리자 기능
* 쿠폰
* 배송
* 리뷰
* 도전 기능

## Package Structure

```text
com.example.paymentsystem

domain.auth
domain.member
domain.product
domain.cart
domain.order
domain.point
domain.payment
domain.refund
domain.webhook

global.common
global.config
global.error
global.response
global.security

infra.portone
```

각 도메인 패키지는 필요에 따라 다음 구조를 가진다.

```text
controller
service
repository
entity
dto
```

## Code Rules

* Entity에 `@Setter` 사용 금지
* Entity 기본 생성자는 `protected`
* Entity 상태 변경은 도메인 메서드로 처리
* DTO는 `record` 사용을 권장한다.
* DTO 변환은 정적 팩토리 메서드 `from()` 방식을 사용한다.
* DTO 생성은 `static from()` 또는 `static of()`를 우선 고려한다.
* Controller에서 Entity 직접 반환 금지
* 공통 응답 형식 사용
* 비즈니스 예외는 `BusinessException`, `ErrorCode` 사용
* 조회는 `@Transactional(readOnly = true)`
* 생성/수정/삭제는 `@Transactional`
* 외부 API 호출을 긴 DB 트랜잭션 안에 넣지 않는다
* 상태값은 문자열 직접 비교 금지
* Enum은 `@Enumerated(EnumType.STRING)` 사용
* 공개 클래스, 메서드, DTO 변환 메서드에는 JavaDoc(`/** */`) 작성
* 복잡한 비즈니스 로직에는 JavaDoc 또는 적절한 주석 작성

## Method Naming

* 조회: `findById`, `findAll`, `findByEmail`
* 생성: `createOrder`, `createPayment`, `createRefund`
* 수정: `updateQuantity`
* 삭제: `removeCartItem`, `clearCart`
* 검증: `validateStock`, `validateOwnership`, `validatePaymentAmount`
* Boolean: `isPaid`, `hasEnoughPoint`, `canRefund`

사용 금지:

* `getAll`
* `select`
* `doProcess`
* `execute`
* `work`

## Domain Rules

### Cart

* Cart와 CartItem은 분리한다.
* 회원 1명은 장바구니 1개를 가진다.
* 장바구니 1개는 여러 장바구니 상품을 가진다.
* 동일 상품 재담기는 수량 합산으로 처리한다.
* DB 제약: `UNIQUE(cart_id, product_id)`

### Order

* 주문은 장바구니 기반으로 생성한다.
* 주문 생성 시 재고를 선차감한다.
* 주문 생성 시 OrderItem에 상품명/가격 스냅샷을 저장한다.
* 주문 생성 시 Payment를 READY 상태로 함께 생성한다.
* 주문 상태:

  * `PAYMENT_PENDING`
  * `ORDER_COMPLETED`
  * `ORDER_CANCELLED`

### Point

* Point 테이블을 별도로 둔다.
* Point는 현재 잔액을 관리한다.
* PointHistory는 거래 이력을 관리한다.
* 포인트 타입:

  * `USE`
  * `EARN`
  * `USE_CANCEL`
  * `EARN_CANCEL`

### Payment

* 서버가 `portonePaymentId`를 생성한다.
* 주문 생성 시 Payment는 `READY` 상태로 생성한다.
* 클라이언트는 `portonePaymentId`로 PortOne 결제창을 호출한다.
* 결제 확정 시 클라이언트는 `orderId`와 `portonePaymentId`를 서버에 전달한다.
* 서버는 DB의 `portonePaymentId`와 요청의 `portonePaymentId`가 일치하는지 검증한다.
* 서버는 PortOne API로 결제 상태와 금액을 조회한다.
* 서버 계산 금액과 PortOne 승인 금액이 일치해야 결제 완료 처리한다.
* 결제 상태:

  * `READY`
  * `PAID`
  * `FAILED`
  * `PARTIAL_REFUNDED`
  * `REFUNDED`

### Refund

* 환불은 주문 상품 ID와 수량 기준으로 요청한다.
* 환불 금액은 서버가 스냅샷 가격 × 수량으로 계산한다.
* Refund와 RefundItem을 저장한다.
* 환불 시 재고를 복구한다.
* 포인트 사용분은 `USE_CANCEL`로 복구한다.
* 포인트 적립분은 `EARN_CANCEL`로 회수한다.

### Webhook

* 웹훅은 JWT 인증을 사용하지 않는다.
* 웹훅 서명을 검증한다.
* 웹훅 본문은 신뢰하지 않는다.
* `portonePaymentId`만 추출하고 PortOne API로 재조회한다.
* 결제 확정 API와 웹훅은 공통 결제 확정 로직을 사용한다.
* 웹훅은 멱등하게 처리한다.

## Review guidelines

모든 리뷰는 한국어로 작성한다.

다음 형식을 따른다.

### 문제점
### 위험성
### 수정 방향

우선적으로 검토한다.

- ERD와 코드 일치 여부
- API 명세와 코드 일치 여부
- Entity Setter 사용 금지
- 트랜잭션 범위 적절성
- N+1 발생 가능성
- 권한 검증 누락
- 결제 금액 검증
- 멱등성 누락
- 타인 데이터 접근 가능성