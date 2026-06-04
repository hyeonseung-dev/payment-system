
# docs/CODE_CONVENTION.md

# Code Convention

## Branch

```text
feature/auth
feature/product
feature/order
feature/payment
````

## Commit

```text
feat: 기능 구현
fix: 버그 수정
refactor: 리팩토링
docs: 문서 수정
test: 테스트
chore: 설정 변경
```

## Entity

* `@Setter` 사용 금지
* 기본 생성자는 protected
* 상태 변경은 도메인 메서드 사용
* Enum은 STRING으로 저장
* BaseEntity 상속
* 공개 클래스 및 메서드에는 JavaDoc(`/** */`) 작성

## DTO

* Request / Response 분리
* DTO는 `record` 사용을 권장
* DTO 변환은 `static from()` 방식 사용
* 필요 시 `static of()` 사용
* Entity 직접 반환 금지
* DTO 및 DTO 변환 메서드에는 JavaDoc 작성

예시:

```java
/**
 * 결제 응답 DTO
 */
public record PaymentResponse(Long paymentId) {

    /**
     * Payment 엔티티를 DTO로 변환한다.
     *
     * @param payment 결제 엔티티
     * @return 결제 응답 DTO
     */
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId());
    }
}
```

## Method Naming

```text
조회 → find
생성 → create
수정 → update
삭제 → remove
전체삭제 → clear
검증 → validate
존재확인 → exists
상태변경 → complete / cancel / refund / deduct / restore
boolean → is / has / can
```

## Transaction

* 조회: `@Transactional(readOnly = true)`
* 쓰기: `@Transactional`
* 외부 API 호출을 긴 DB 트랜잭션 안에 넣지 않는다

## Review Checklist

* API 명세와 URL이 일치하는가
* 공통 응답 형식을 사용하는가
* 타인 자원 접근을 막는가
* 트랜잭션이 적절한가
* Entity Setter를 사용하지 않았는가
* 상태값 문자열 비교를 하지 않았는가
* DTO가 `record` 기반으로 작성되었는가
* DTO가 `static from()` 패턴을 사용하는가
* JavaDoc이 작성되어 있는가
* 재고/포인트/결제 정합성이 유지되는가

