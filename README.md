# Payment System

## 목차

1. [프로젝트 소개](#프로젝트-소개)
2. [주요 기능](#주요-기능)
3. [개발 기간](#개발-기간)
4. [기술 스택](#기술-스택)
5. [개발 컨벤션](#개발-컨벤션)
6. [담당 도메인](#담당-도메인)
7. [프로젝트 파일 구조](#프로젝트파일-구조)
8. [트러블 슈팅](#트러블-슈팅)
9. [문서](#문서)

---

## 프로젝트 소개

Spring Boot와 PortOne V2를 활용한 커머스 결제 시스템 팀 프로젝트입니다.

회원가입, 로그인, 상품 조회, 장바구니, 주문, 포인트, 결제, 환불, 웹훅까지 결제 중심의 핵심 흐름을 구현했습니다. JWT 기반 인증을 적용하고, 주문/결제/환불 과정에서 서버 데이터와 PortOne 결제 정보를 검증하여 금액 및 상태 정합성을 유지하는 것을 목표로 했습니다.

구현 제외 범위는 구독, 멤버십 등급, 관리자 기능, 쿠폰, 배송, 리뷰입니다.

---

## 주요 기능

1. **인증 / 회원** - 회원가입, 로그인, 로그아웃, JWT 발급 및 인증 처리
2. **상품** - 상품 목록 조회, 상품 상세 조회
3. **장바구니** - 장바구니 조회, 상품 추가, 수량 변경, 상품 삭제
4. **주문** - 주문서 미리보기, 장바구니 기반 주문 생성, 상품 바로 주문 생성, 주문 목록/상세 조회
5. **포인트** - 포인트 잔액 조회, 포인트 거래 내역 조회, 결제/환불 흐름에서 포인트 사용 및 복구 처리
6. **결제** - 주문 생성 시 Payment READY 생성, PortOne 결제창 연동 정보 제공, 결제 확정, 결제 전체 취소
7. **환불** - 주문 상품 단위 환불, 서버 기준 환불 금액 계산, 환불 수량 검증, 재고/포인트 정합성 처리
8. **웹훅** - PortOne 웹훅 서명 검증, 결제 완료/취소 웹훅 처리, 중복 웹훅 멱등 처리
9. **공통 시스템** - 공통 응답 포맷, GlobalExceptionHandler, ErrorCode 기반 비즈니스 예외, BaseEntity, Spring Security 설정

---

## 개발 기간

2026.05.27 ~ 2026.06.09

---

## 기술 스택

| 분류 | 사용 기술 |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 4.0.6, Spring MVC, Spring Data JPA, Spring Security |
| Auth | JWT |
| Database | MySQL, H2 Test |
| Payment | PortOne V2 Server SDK |
| Build | Gradle |
| Test | JUnit 5 |
| 협업 | GitHub, Postman |

---

## 개발 컨벤션

### Git Flow

```text
main
└── develop
    ├── feature/common-auth-deploy
    ├── feature/product-point
    ├── feature/cart-order
    └── feature/payment-refund-webhook
```

| 브랜치 | 역할 |
| --- | --- |
| `main` | 최종 제출 및 배포 기준 브랜치 |
| `develop` | 기능 통합 브랜치 |
| `feature/*` | 도메인 또는 기능 단위 작업 브랜치 |

### Git 규칙

- `main` 직접 push 금지
- `develop` 직접 push 금지
- feature 브랜치에서 작업
- PR 생성 후 승인 1개 이상 시 merge

### 커밋 컨벤션

```text
<타입>: <작업 내용>
```

| 타입 | 설명 |
| --- | --- |
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `refactor` | 코드 리팩토링 |
| `test` | 테스트 코드 |
| `chore` | 설정 변경 |

### 코드 작성 규칙

- Entity에 `@Setter` 사용 금지
- Entity 기본 생성자는 `protected`
- Entity 상태 변경은 도메인 메서드로 처리
- Controller에서 Entity 직접 반환 금지
- DTO는 `record` 사용 권장
- DTO 변환은 `static from()` 또는 `static of()` 우선 사용
- 비즈니스 예외는 `BusinessException`, `ErrorCode` 사용
- 조회 메서드는 `@Transactional(readOnly = true)` 사용
- 생성/수정/삭제 메서드는 `@Transactional` 사용
- 외부 API 호출은 긴 DB 트랜잭션 안에 넣지 않음
- 회원별 리소스 조회/수정/삭제 시 소유자 검증 수행
- 결제/환불/웹훅 처리는 멱등성을 고려

---

## 담당 도메인

| 담당자 | 범위 |
| --- | --- |
| 김준형 | 공통, 인증, 회원, 보안, 배포 |
| 이지영 | 상품, 포인트 |
| 이지현 | 장바구니, 주문 |
| 김현승 | 결제, 환불, 웹훅, PortOne 연동 |

---

## 프로젝트파일 구조

```text
com.example.paymentsystem
├── domain
│   ├── auth
│   │   ├── controller
│   │   ├── service
│   │   └── dto
│   ├── member
│   │   ├── entity
│   │   ├── repository
│   │   └── service
│   ├── product
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   ├── cart
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   ├── order
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   ├── point
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   ├── payment
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   ├── refund
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── dto
│   └── webhook
│       ├── controller
│       ├── service
│       ├── repository
│       ├── entity
│       └── dto
├── global
│   ├── common
│   │   └── BaseEntity
│   ├── config
│   ├── error
│   ├── exception
│   ├── response
│   └── security
│       ├── filter
│       ├── handler
│       └── jwt
└── infra
    └── portone
```
        
---

## 트러블 슈팅

# 🚨 Trouble Shooting

## 1. 장바구니 생성 시점 결정

### 문제

장바구니를 회원가입 시 미리 생성할지, 상품을 처음 담는 시점에 생성할지 고민이 있었다.

### 해결

장바구니를 실제 사용 시점에 생성하는 방식을 선택했다.

* 사용하지 않는 장바구니 데이터 생성을 방지
* 실제 사용자 행동과 데이터 생성 시점 일치
* `getOrCreateCart()` 방식으로 간결하게 구현

---

## 2. 주문 생성 시 재고 차감 시점 결정

### 문제

재고를 결제 완료 후 차감할지, 주문 생성 시 선차감할지 고민이 있었다.

### 해결

주문 생성 시점에 재고를 선차감하는 방식을 선택했다.

* 동시 주문 상황에서 초과 판매 방지
* 주문 의사가 확정된 시점에 재고 선점
* 결제 실패 시 재고 복구 로직 추가

---

## 3. 주문 생성 시 락 전략 선택

### 문제

동일 상품에 대한 주문이 동시에 발생할 경우 재고 정합성이 깨질 수 있었다.

### 해결

주문 생성 과정에서는 `PESSIMISTIC_WRITE`를 적용했다.

* 재고 선차감 구조와 궁합이 좋음
* 충돌 비용이 큰 주문 도메인에 적합
* 재고 정합성을 우선 보장

---

## 4. 환불 요청 중복 처리 문제

### 문제

동일 결제 건에 대해 여러 환불 요청이 동시에 들어오면 PortOne 환불 API가 중복 호출될 수 있었다.

### 해결

* 환불 요청 시 `Refund`를 `REQUESTED` 상태로 먼저 저장하여 환불 수량을 선점
* `Payment`를 `PESSIMISTIC_WRITE`로 조회하여 동일 결제 건에 대한 동시 환불 차단

---

## 5. PortOne 타임아웃 발생 시 중복 환불 위험

### 문제

PortOne 환불은 성공했지만 네트워크 타임아웃으로 응답을 받지 못하는 경우 동일 환불 요청이 다시 전송될 수 있었다.

### 해결

* 환불 요청마다 `Idempotency-Key` 생성
* 동일 Key를 PortOne에 전달하여 중복 요청 방지

---

## 6. 웹훅 재전송으로 인한 중복 처리 문제

### 문제

PortOne은 동일 이벤트에 대해 웹훅을 여러 번 전송할 수 있다. 이미 처리된 취소 웹훅이 다시 도착하면 불필요한 예외와 로그가 발생했다.

### 해결

결제 상태가 이미 아래 상태인 경우 추가 처리하지 않고 무시하도록 구현했다.

* `REFUNDED`
* `CANCELLED`

웹훅 상태는 `IGNORED`로 기록하여 멱등성을 보장했다.

---

## 7. 장바구니 삭제 시점 개선

### 문제

주문 생성 직후 장바구니를 비우면 결제 실패 시 사용자가 상품을 다시 담아야 하는 문제가 있었다.

### 해결

장바구니 삭제 시점을 주문 생성이 아닌 결제 확정 시점으로 변경했다.

---

## 8. 주문 정보 스냅샷 저장

### 문제

상품명이나 가격이 변경되면 과거 주문 내역도 함께 변경되는 문제가 발생할 수 있었다.

### 해결

`OrderItem`에 주문 당시의 상품명, 가격, 수량을 스냅샷으로 저장했다.

* 주문 당시 정보 보존
* 환불 금액 계산 기준 유지
* 주문 이력의 신뢰성 확보

---

## 문서

| 문서 | 설명 |
| --- | --- |
| [API 명세](docs/API.md) | 실제 Controller/DTO 기준 전체 API 명세 |
| [API 요약](docs/API_SPEC.md) | 도메인별 API 목록 요약 |
| [ERD](docs/ERD.md) | 테이블 및 주요 관계 정리 |
| [ERDCloud Import DDL](docs/ERD_CLOUD.sql) | ERDCloud에 import 가능한 MySQL DDL |
| [Flow](docs/FLOW.md) | 전체 사용자 플로우 및 결제 흐름 |
| [Mermaid Flow](docs/MERMAID_FLOW.md) | 전체/도메인별 Mermaid 플로차트 및 시퀀스 다이어그램 |
| [System Architecture](docs/SA.md) | 구현 범위, 제외 범위, 핵심 정책 |
| [Code Convention](docs/CODE_CONVENTION.md) | 코드 작성 규칙 및 리뷰 체크리스트 |

---
