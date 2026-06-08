# Mermaid Flow

발표 자료와 README 문서에 붙여넣기 좋은 Mermaid 다이어그램 모음입니다.

## [전체] 유저 플로우

```mermaid
flowchart TD
    A([시작]) --> B[회원가입]
    B --> C[로그인]
    C --> D[JWT Access Token 발급]
    D --> E[상품 목록 조회]
    E --> F{구매 방식 선택}

    F -->|장바구니 구매| G[장바구니 담기]
    G --> H[장바구니 조회]
    H --> I[주문서 미리보기]
    I --> J[장바구니 기반 주문 생성]

    F -->|바로 구매| K[상품 바로 주문 생성]

    J --> L[Payment READY 생성]
    K --> L
    L --> M[portonePaymentId 응답]
    M --> N{PG 결제 금액 있음?}

    N -->|pgAmount > 0| O[PortOne 결제창 호출]
    O --> P[카드 결제 완료]
    N -->|pgAmount = 0| Q[포인트 전액 결제]

    P --> R[결제 확정 API 호출]
    Q --> R
    R --> S[PortOne 결제 상태/금액 검증]
    S --> T{검증 성공?}
    T -->|성공| U[주문 COMPLETED / 결제 PAID]
    T -->|실패| V[결제 FAILED / 주문 취소 처리]

    U --> W{환불 요청?}
    W -->|아니오| X([구매 완료])
    W -->|예| Y[환불 요청]
    Y --> Z[환불 수량/금액 검증]
    Z --> AA[PortOne 환불 요청]
    AA --> AB[재고 복구 / 포인트 복구 및 회수]
    AB --> AC[Payment PARTIAL_REFUNDED 또는 REFUNDED]
    AC --> AD([환불 완료])
```

## [전체] 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Client as 클라이언트
    participant API as Spring Boot API
    participant DB as MySQL
    participant PortOne as PortOne API

    User->>Client: 회원가입/로그인
    Client->>API: POST /api/auth/login
    API->>DB: 회원 조회 및 비밀번호 검증
    DB-->>API: 회원 정보
    API-->>Client: accessToken

    User->>Client: 상품 선택
    Client->>API: GET /api/products
    API->>DB: 상품 목록 조회
    DB-->>API: 상품 목록
    API-->>Client: 상품 목록

    alt 장바구니 주문
        Client->>API: POST /api/carts/items
        API->>DB: 장바구니 생성/상품 추가/재고 검증
        DB-->>API: cartItemId
        API-->>Client: 장바구니 담기 결과
        Client->>API: POST /api/orders
    else 상품 바로 주문
        Client->>API: POST /api/orders/products
    end

    API->>DB: 회원/상품/장바구니 조회 및 락
    API->>DB: 재고 선차감
    API->>DB: Order PAYMENT_PENDING 저장
    API->>DB: OrderItem 스냅샷 저장
    API->>DB: Payment READY 저장 earnedPointAmount=pgAmount 1%
    API->>DB: 포인트 사용 시 PointHistory USE 저장
    API-->>Client: orderId, paymentId, portonePaymentId, pgAmount

    Client->>API: GET /api/portone/config
    API-->>Client: storeId, channelKey

    alt pgAmount > 0
        Client->>PortOne: 결제창 호출 portonePaymentId
        PortOne-->>Client: 결제 완료 콜백
    else pgAmount = 0
        Client->>Client: 결제창 생략
    end

    Client->>API: POST /api/payments/confirm
    API->>DB: Payment + Order 조회
    API->>API: 소유권 및 portonePaymentId 검증
    opt pgAmount > 0
        API->>PortOne: 결제 정보 재조회
        PortOne-->>API: status, paidAmount
        API->>API: 결제 상태/금액 검증
    end
    API->>DB: Payment PAID, Order COMPLETED
    API->>DB: 적립 포인트 PointHistory EARN 저장
    API->>DB: 장바구니 주문 항목 삭제
    API-->>Client: 결제 확정 결과

    PortOne-->>API: POST /api/webhooks/portone
    API->>API: 웹훅 서명 검증
    API->>DB: webhookId 중복 검증 및 저장
    API->>PortOne: 결제 정보 재조회
    PortOne-->>API: 결제 정보
    API->>DB: 결제 확정/취소 동기화 멱등 처리
    API-->>PortOne: 200 OK
```

## [장바구니] 장바구니 담기 및 재고 수정 흐름

```mermaid
flowchart TD
    A([POST /api/carts/items]) --> B{JWT 인증 회원인가?}
    B -->|아니오| B1[AUTH_001 인증 필요]
    B -->|예| C[상품 조회]
    C --> D{상품 존재?}
    D -->|아니오| D1[PRODUCT_001 상품 없음]
    D -->|예| E[회원 장바구니 조회]
    E --> F{장바구니 존재?}
    F -->|아니오| G[장바구니 생성]
    F -->|예| H[기존 장바구니 사용]
    G --> I[동일 상품 CartItem 조회]
    H --> I
    I --> J{이미 담긴 상품인가?}
    J -->|예| K[기존 수량 + 요청 수량 계산]
    J -->|아니오| L[요청 수량 사용]
    K --> M{상품 재고 초과?}
    L --> M
    M -->|예| M1[CART_005 재고 초과]
    M -->|아니오| N[CartItem 생성 또는 수량 증가]
    N --> O[CartItem 저장]
    O --> P([장바구니 담기 완료])
```

## [주문] 주문서 미리보기 흐름

```mermaid
flowchart TD
    A([POST /api/orders/preview]) --> B{cartItemIds 비어있음?}
    B -->|예| B1[COMMON_001 검증 실패]
    B -->|아니오| C[인증 회원의 CartItem 목록 조회]
    C --> D{요청한 CartItem 모두 조회됨?}
    D -->|아니오| D1[CART_002 장바구니 상품 없음]
    D -->|예| E[상품명/가격/수량 조회]
    E --> F[상품별 subtotal 계산]
    F --> G[totalAmount 합산]
    G --> H([주문서 미리보기 응답])
```

## [주문] 상품 주문 및 상품 선차감

```mermaid
flowchart TD
    A([POST /api/orders/products]) --> B{JWT 인증 회원인가?}
    B -->|아니오| B1[AUTH_001 인증 필요]
    B -->|예| C[회원 조회 및 락]
    C --> D[상품 조회 및 락]
    D --> E{상품 존재?}
    E -->|아니오| E1[PRODUCT_001 상품 없음]
    E -->|예| F{요청 수량 재고 충분?}
    F -->|아니오| F1[PRODUCT_002 재고 부족]
    F -->|예| G[totalAmount = 가격 x 수량]
    G --> H[사용 포인트 검증]
    H --> I{포인트 사용 가능?}
    I -->|아니오| I1[POINT_002 또는 POINT_004]
    I -->|예| J[회원 포인트 선차감]
    J --> K[pgAmount 계산]
    K --> L[상품 재고 선차감]
    L --> M[Order PAYMENT_PENDING 생성]
    M --> N[OrderItem 상품 스냅샷 저장]
    N --> O[Payment READY 생성]
    O --> P([주문 생성 응답 portonePaymentId])
```

## [주문] 주문 취소 및 상품 선차감 복구

```mermaid
flowchart TD
    A([주문 취소 요청]) --> B[주문 조회]
    B --> C{주문 소유자 일치?}
    C -->|아니오| C1[AUTH_004 접근 권한 없음]
    C -->|예| D{주문 상태가 PAYMENT_PENDING?}
    D -->|아니오| D1[ORDER_002 유효하지 않은 주문 상태]
    D -->|예| E[Order CANCELED 변경]
    E --> F[OrderItem 목록 조회]
    F --> G[각 상품 row 락 조회]
    G --> H[주문 수량만큼 상품 재고 복구]
    H --> I{사용 포인트 있음?}
    I -->|예| J[사용 포인트 복구]
    I -->|아니오| K[포인트 처리 생략]
    J --> L[PointHistory USE_CANCEL 저장]
    K --> M([주문 취소 완료])
    L --> M
```

> 현재 코드에는 주문 취소 Controller가 없습니다. 위 흐름은 `Order.cancel()` 도메인 규칙과 주문 생성 시 재고 선차감 정책을 기준으로 한 구현 예정/설계 플로우입니다.

## [인증] JWT 인증 흐름

```mermaid
flowchart TD
    A([POST /api/auth/login]) --> B[이메일로 회원 조회]
    B --> C{회원 존재?}
    C -->|아니오| C1[MEMBER_003 인증 실패]
    C -->|예| D[BCrypt 비밀번호 검증]
    D --> E{비밀번호 일치?}
    E -->|아니오| E1[MEMBER_003 인증 실패]
    E -->|예| F[memberId 기반 JWT 생성]
    F --> G[LoginResponse accessToken, tokenType 반환]
    G --> H([로그인 완료])
```

## [인증] 인증이 필요한 API 흐름

```mermaid
flowchart TD
    A([인증 필요 API 요청]) --> B[Authorization 헤더 확인]
    B --> C{Bearer 토큰 존재?}
    C -->|아니오| C1[인증 정보 없음]
    C -->|예| D[Bearer prefix 제거]
    D --> E{블랙리스트 토큰인가?}
    E -->|예| E1[인증 실패]
    E -->|아니오| F{JWT 유효성 검증}
    F -->|실패| F1[인증 실패]
    F -->|성공| G[토큰에서 memberId 추출]
    G --> H[SecurityContext에 Authentication 저장]
    H --> I[Controller에서 memberId 사용]
    I --> J[소유권 검증]
    J --> K{소유자 일치?}
    K -->|아니오| K1[AUTH_004 접근 권한 없음]
    K -->|예| L([비즈니스 로직 실행])
```

## [결제] 일반 카드 결제 확정 흐름

```mermaid
flowchart TD
    A([POST /api/payments/confirm]) --> B[Payment + Order 조회]
    B --> C{결제 정보 존재?}
    C -->|아니오| C1[PAYMENT_001 결제 정보 없음]
    C -->|예| D[주문 소유권 검증]
    D --> E{소유자 일치?}
    E -->|아니오| E1[AUTH_004 접근 권한 없음]
    E -->|예| F[요청 portonePaymentId와 DB 값 비교]
    F --> G{일치?}
    G -->|아니오| G1[PAYMENT_006 결제 ID 불일치]
    G -->|예| H{이미 PAID?}
    H -->|예| H1[기존 결제 완료 응답 반환]
    H -->|아니오| I[PortOne 결제 정보 조회]
    I --> J{PortOne status == PAID?}
    J -->|아니오| J1[Payment FAILED / Order 실패 처리]
    J -->|예| K{DB pgAmount == PortOne paidAmount?}
    K -->|아니오| K1[PortOne 보상취소 + 결제 실패 처리]
    K -->|예| L[Payment PAID 변경]
    L --> M[Order COMPLETED 변경]
    M --> N[장바구니 주문 항목 삭제]
    N --> O([결제 확정 완료])
```

## [결제] 포인트 + 카드 복합 결제 흐름

```mermaid
flowchart TD
    A([주문 생성 요청]) --> B[totalAmount 계산]
    B --> C[usePointAmount 확인]
    C --> D{usePointAmount > totalAmount?}
    D -->|예| D1[POINT_004 사용할 수 없는 포인트]
    D -->|아니오| E{회원 포인트 충분?}
    E -->|아니오| E1[POINT_002 포인트 부족]
    E -->|예| F[pgAmount = totalAmount - usePointAmount]
    F --> G[earnedPointAmount = pgAmount 1%]
    G --> H[Payment READY 생성]
    H --> H1[포인트 사용 시 PointHistory USE 저장]
    H1 --> I{pgAmount > 0?}
    I -->|예| J[PortOne 결제창 카드 결제]
    I -->|아니오| K[카드 결제창 생략 가능]
    J --> L[결제 확정 API]
    K --> L
    L --> M{pgAmount > 0?}
    M -->|예| N[PortOne 상태/금액 검증]
    M -->|아니오| O[PortOne 조회 생략]
    N --> P[Payment PAID / Order COMPLETED]
    O --> P
    P --> Q[PointHistory EARN 저장]
    Q --> R([복합 결제 완료])
```

## [결제] 웹훅 멱등 동기화 흐름

```mermaid
flowchart TD
    A([POST /api/webhooks/portone]) --> B[webhook-id / timestamp / signature 수신]
    B --> C[PortOne 웹훅 서명 검증]
    C --> D{서명 유효?}
    D -->|아니오| D1[처리 중단 후 200 OK]
    D -->|예| E[portonePaymentId 추출]
    E --> F[webhookId 기존 이벤트 조회]
    F --> G{기존 이벤트 존재?}
    G -->|COMPLETED 또는 IGNORED| G1[중복 웹훅 무시]
    G -->|FAILED| H[실패 이벤트 재처리]
    G -->|없음| I[WebhookEvent RECEIVED 저장]
    H --> J[이벤트 타입 확인]
    I --> J
    J --> K{Transaction.Paid?}
    K -->|예| L[PortOne 결제 재조회]
    L --> M[결제 확정 공통 로직 호출]
    M --> N[WebhookEvent COMPLETED]
    K -->|아니오| O{Transaction.Cancelled?}
    O -->|예| P[PortOne 취소 상태 재조회]
    P --> Q[결제취소 동기화 로직 호출]
    Q --> N
    O -->|아니오| R[WebhookEvent IGNORED]
    N --> S([200 OK])
    R --> S
```

## [환불] 부분 환불 흐름

```mermaid
flowchart TD
    A([POST /api/payments/{paymentId}/refunds]) --> B[Payment row 락 조회]
    B --> C[결제 소유권 검증]
    C --> D{소유자 일치?}
    D -->|아니오| D1[AUTH_004 접근 권한 없음]
    D -->|예| E{Payment 상태 PAID 또는 PARTIAL_REFUNDED?}
    E -->|아니오| E1[REFUND_002 환불 불가 상태]
    E -->|예| F[중복 orderItemId 검증]
    F --> G[OrderItem 조회 및 주문 소속 검증]
    G --> H[기존 REQUESTED/COMPLETED 환불 수량 합산]
    H --> I{잔여 환불 수량 초과?}
    I -->|예| I1[REFUND_003 수량 초과]
    I -->|아니오| J[스냅샷 가격 x 수량으로 환불 금액 계산]
    J --> K[포인트 환불액 / PG 환불액 계산]
    K --> L[Refund REQUESTED 저장]
    L --> M[RefundItem 저장]
    M --> N{pgRefundAmount > 0?}
    N -->|예| O[PortOne 환불 요청]
    N -->|아니오| P[PortOne 호출 생략]
    O --> Q{PortOne 환불 성공?}
    Q -->|실패| Q1[Refund FAILED 처리]
    Q -->|성공| R[Refund COMPLETED 처리]
    P --> R
    R --> S[환불 수량만큼 재고 복구]
    S --> T[사용 포인트 복구]
    T --> U[적립 포인트 회수]
    U --> V[Payment PARTIAL_REFUNDED 또는 REFUNDED]
    V --> W{전액 환불?}
    W -->|예| X[Order CANCELED]
    W -->|아니오| Y[주문 상태 유지]
    X --> Z([환불 완료])
    Y --> Z
```

## [상품] 상품 주문 흐름

```mermaid
flowchart TD
    A([상품 상세 조회]) --> B[구매 수량 선택]
    B --> C[상품 바로 주문 API 호출]
    C --> D[상품 row 비관적 락]
    D --> E[재고 검증]
    E --> F[포인트 사용 검증]
    F --> G[상품 재고 선차감]
    G --> H[주문 생성]
    H --> I[주문 상품 스냅샷 저장]
    I --> J[Payment READY 생성]
    J --> K[PortOne 결제창 호출 또는 포인트 전액 결제]
    K --> L[결제 확정]
    L --> M([상품 주문 완료])
```

## [포인트] 포인트 잔액 ↔ 원장 동기화 흐름

```mermaid
flowchart TD
    A([포인트 변동 이벤트]) --> B{변동 타입}
    B -->|USE| C[회원 pointBalance 차감]
    B -->|EARN| D[회원 pointBalance 증가]
    B -->|USE_CANCEL| E[회원 pointBalance 복구]
    B -->|EARN_CANCEL| F[회원 pointBalance 회수]

    C --> G{잔액 음수?}
    G -->|예| G1[POINT_002 또는 POINT_003]
    G -->|아니오| H[PointHistory USE 저장]

    D --> I[PointHistory EARN 저장]
    E --> J[PointHistory USE_CANCEL 저장]
    F --> K[PointHistory EARN_CANCEL 저장]

    H --> L[balanceAfter = 변경 후 pointBalance]
    I --> L
    J --> L
    K --> L
    L --> M[회원 잔액과 원장 잔액 일치]
    M --> N([포인트 동기화 완료])
```
