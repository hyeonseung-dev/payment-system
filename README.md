# Payment System

## 목차

1. [프로젝트 소개](#프로젝트-소개)
2. [주요 기능](#주요-기능)
3. [개발 기간](#개발-기간)
4. [기술 스택](#기술-스택)
5. [개발 컨벤션](#개발-컨벤션)
6. [담당 도메인](#담당-도메인)
7. [문서](#문서)

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
