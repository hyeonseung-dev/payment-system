# Payment System

커머스 결제 시스템 팀 프로젝트

## 기술 스택

* Java 17
* Spring Boot
* Spring Security
* Spring Data JPA
* MySQL
* JWT
* PortOne

## 브랜치 전략

```text
main
└─ develop
   ├─ feature/common-auth-deploy
   ├─ feature/product-point
   ├─ feature/cart-order
   └─ feature/payment-refund-webhook
```

## Git 규칙

* main 직접 push 금지
* develop 직접 push 금지
* feature 브랜치에서 작업
* PR 생성 후 승인 1개 이상 시 merge

## 담당 도메인

* 김준형 : 공통, 인증, 배포
* 이지영 : 상품, 포인트
* 이지현 : 장바구니, 주문
* 김현승 : 결제, 환불, 웹훅
