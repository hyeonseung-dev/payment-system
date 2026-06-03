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

## Implementation Principles

* 필수 기능 구현을 우선하며, 과제 범위를 벗어나는 기능은 추가하지 않는다.
* 요구사항에 없는 관리자 기능, 쿠폰, 배송, 리뷰, 구독, 멤버십 기능은 임의로 만들지 않는다.
* 비즈니스 로직은 Controller가 아닌 Service 또는 Domain Entity에 둔다.
* Controller는 요청 검증, 인증 사용자 식별, Service 호출, DTO 응답 반환만 담당한다.
* Entity는 DB 테이블 표현과 도메인 상태 변경 책임만 가진다.
* 외부 API 응답값을 그대로 신뢰하지 않고 서버 데이터와 반드시 검증한다.
* 금액, 수량, 포인트, 재고는 음수가 될 수 없다.
* 결제/환불/포인트/재고 변경은 데이터 정합성을 최우선으로 처리한다.

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

## Security Rules

* JWT가 필요한 API는 인증된 사용자만 접근할 수 있어야 한다.
* 회원별 리소스 조회/수정/삭제 시 반드시 소유자 검증을 수행한다.
* 요청의 `memberId`, `orderId`, `cartId`, `paymentId`만 믿고 처리하지 않는다.
* 인증 사용자 정보와 DB 소유자 정보를 비교해 타인 데이터 접근을 차단한다.
* 비밀번호는 반드시 암호화해서 저장한다.
* 민감 정보는 로그에 남기지 않는다.
* 웹훅 API는 JWT 인증 대상에서 제외하되, 반드시 서명 검증을 수행한다.

## Transaction Rules

* 조회 메서드는 `@Transactional(readOnly = true)`를 사용한다.
* 생성/수정/삭제 메서드는 `@Transactional`을 사용한다.
* 외부 API 호출은 긴 DB 트랜잭션 안에서 수행하지 않는다.
* PortOne API 조회/환불 요청과 DB 상태 변경은 트랜잭션 경계를 명확히 분리한다.
* 재고 차감, 포인트 차감, 결제 완료, 환불 완료처럼 정합성이 중요한 로직은 중복 실행에 안전해야 한다.
* 결제 확정과 웹훅 처리는 멱등성을 보장해야 한다.

## Validation Rules

* 요청 DTO에는 필요한 검증 어노테이션을 사용한다.
* 수량은 1 이상이어야 한다.
* 금액은 0 이상이어야 한다.
* 장바구니 상품 수량은 재고를 초과할 수 없다.
* 주문 생성 시점에 재고를 다시 검증한다.
* 결제 확정 시 서버 계산 금액과 PortOne 승인 금액을 반드시 비교한다.
* 환불 요청 수량은 주문 수량과 기존 환불 수량을 초과할 수 없다.

## Response Rules

* Controller는 Entity를 직접 반환하지 않는다.
* 모든 API는 공통 응답 형식을 사용한다.
* 성공 응답과 실패 응답의 구조를 일관되게 유지한다.
* 예외 메시지는 클라이언트가 이해할 수 있어야 하지만, 내부 구현 정보는 노출하지 않는다.

## Repository Rules

* 단순 조회는 Spring Data JPA 메서드 네이밍을 우선 사용한다.
* 복잡한 조회가 필요한 경우 명확한 목적을 가진 쿼리 메서드를 작성한다.
* N+1 가능성이 있는 조회는 `fetch join`, `@EntityGraph`, DTO 조회 등을 고려한다.
* Optional 반환값은 Service 계층에서 명확하게 예외 처리한다.

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
* 주문 생성 시 Payment를 `READY` 상태로 함께 생성한다.
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
* 결제 완료 처리는 중복 호출되어도 안전해야 한다.
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
* 환불 요청 수량은 주문 수량과 기존 환불 수량을 초과할 수 없다.
* 환불 시 재고를 복구한다.
* 포인트 사용분은 `USE_CANCEL`로 복구한다.
* 포인트 적립분은 `EARN_CANCEL`로 회수한다.
* 환불 처리는 중복 호출되어도 안전해야 한다.

### Webhook

* 웹훅은 JWT 인증을 사용하지 않는다.
* 웹훅 서명을 검증한다.
* 웹훅 본문은 신뢰하지 않는다.
* `portonePaymentId`만 추출하고 PortOne API로 재조회한다.
* 결제 확정 API와 웹훅은 공통 결제 확정 로직을 사용한다.
* 웹훅은 멱등하게 처리한다.

## Test Rules

* 핵심 비즈니스 로직은 테스트를 작성한다.
* 결제 금액 검증 테스트를 작성한다.
* 타인 주문/장바구니/결제 접근 차단 테스트를 작성한다.
* 재고 차감 및 복구 테스트를 작성한다.
* 포인트 사용, 적립, 취소 테스트를 작성한다.
* 환불 수량 초과 요청 테스트를 작성한다.
* 결제 확정과 웹훅 멱등성 테스트를 작성한다.

## Review guidelines

모든 리뷰는 한국어로 작성한다.

다음 형식을 따른다.

### 문제점

### 위험성

### 수정 방향

우선적으로 검토한다.

* ERD와 코드 일치 여부
* API 명세와 코드 일치 여부
* Entity Setter 사용 금지
* Entity 기본 생성자 `protected` 여부
* Entity 상태 변경이 도메인 메서드로 처리되는지
* Controller에서 Entity 직접 반환 여부
* 공통 응답 형식 사용 여부
* 비즈니스 예외가 `BusinessException`, `ErrorCode`를 사용하는지
* 트랜잭션 범위 적절성
* 외부 API 호출이 긴 DB 트랜잭션 안에 포함되었는지
* N+1 발생 가능성
* JWT 인증 누락
* 권한 검증 누락
* 타인 데이터 접근 가능성
* 결제 금액 검증
* `portonePaymentId` 검증
* 결제 확정 멱등성 누락
* 웹훅 멱등성 누락
* 환불 금액 서버 계산 여부
* 환불 수량 검증 여부
* 포인트 복구/회수 처리 정확성
* 재고 차감/복구 정합성

리뷰 시 단순 취향이나 사소한 스타일 지적보다 실제 장애, 보안 문제, 데이터 정합성 문제, 결제/환불 사고 가능성을 우선한다.
문제가 없다면 억지로 지적하지 않고 특이사항 없음이라고 작성한다.