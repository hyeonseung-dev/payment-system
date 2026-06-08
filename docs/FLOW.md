# docs/FLOW.md

# Flow

## 전체 사용자 플로우

```mermaid
sequenceDiagram
actor User
participant Server as Spring Boot
participant PortOne as PortOne API

User->>Server: POST /api/carts/items
Server-->>User: 장바구니 담기 완료

User->>Server: GET /api/carts
Server-->>User: 장바구니 조회

User->>Server: POST /api/carts/orders cartItemIds, usePointAmount
Server->>Server: 장바구니 조회
Server->>Server: 주문 총액 계산
Server->>Server: 포인트 잔액 조회
Server->>Server: 사용 포인트 검증
Server->>Server: PG 실결제 금액 계산
Server->>Server: 적립 예정 포인트 계산 pgAmount 1%
Server->>Server: 재고 검증
Server->>Server: 재고 선차감
Server->>Server: Order 생성 PAYMENT_PENDING
Server->>Server: OrderItem 스냅샷 저장
Server->>Server: Payment 생성 READY
Server->>Server: portonePaymentId 생성
Server-->>User: orderId, orderNumber, totalAmount, usePointAmount, pgAmount, portonePaymentId

alt pgAmount > 0
    User->>PortOne: 결제창 호출 portonePaymentId, pgAmount
    PortOne-->>User: 결제 완료 콜백
else pgAmount == 0
    User->>User: 포인트 전액 결제, 결제창 생략
end

User->>Server: POST /api/payments/confirm orderId, portonePaymentId
Server->>Server: DB portonePaymentId와 요청 portonePaymentId 비교
Server->>PortOne: portonePaymentId로 결제 정보 조회
PortOne-->>Server: 결제 상태, 결제 금액
Server->>Server: 결제 상태/금액/멱등성 검증
Server->>Server: Order COMPLETED
Server->>Server: Payment PAID
Server->>Server: 포인트 적립 처리
Server->>Server: PointHistory 저장
Server->>Server: 장바구니 비우기
Server-->>User: 결제 완료

User->>Server: POST /api/payments/{paymentId}/cancel reason
Server->>Server: 결제 소유권/상태 검증
Server->>PortOne: 결제 전체 취소 요청
PortOne-->>Server: 결제취소 결과
Server->>Server: Order CANCELED
Server->>Server: Payment CANCELLED
Server->>Server: 재고 복구
Server->>Server: 포인트 사용복구/적립회수
Server->>Server: PointHistory 저장
Server-->>User: 결제취소 완료

PortOne->>Server: POST /api/webhooks/portone
Server->>Server: 웹훅 서명 검증
Server->>Server: webhookId 멱등성 검증
Server->>Server: portonePaymentId 추출
Server->>PortOne: 결제 정보 재조회
PortOne-->>Server: 결제 정보
Server->>Server: 결제 확정 공통 로직 호출
Server-->>PortOne: 200 OK

User->>Server: POST /api/payments/{paymentId}/refunds
Server->>Server: 소유권 검증
Server->>Server: 환불 가능 수량 검증
Server->>Server: 스냅샷 기준 환불 금액 계산
Server->>Server: 포인트/PG 환불 금액 분리
Server->>Server: Refund 저장
Server->>Server: RefundItem 저장
Server->>Server: 재고 복구
Server->>Server: 포인트 사용복구/적립회수
Server->>PortOne: PG 환불 요청
PortOne-->>Server: 환불 결과
Server-->>User: 환불 완료
````
