-- 결제 확정 API 로컬 테스트용 SQL
-- 1. 아래 변수의 실제 PortOne 결제 ID를 테스트 결제 ID로 변경한다.
-- 2. local 프로필 DB에 실행한다.
-- 3. 로그인은 별도 회원가입 또는 기존 회원 JWT를 사용한다.

SET @member_id = 1;
SET @order_number = 'ORDER-TEST-PAYMENT-CONFIRM';
SET @portone_payment_id = 'pay_test_001';
SET @total_amount = 60000;
SET @point_amount = 5000;
SET @pg_amount = 55000;
SET @earned_point_amount = 550;

INSERT INTO orders (
    member_id,
    order_number,
    total_amount,
    use_point_amount_snapshot,
    status,
    created_at,
    updated_at
)
SELECT
    @member_id,
    @order_number,
    @total_amount,
    @point_amount,
    'PAYMENT_PENDING',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM orders
    WHERE order_number = @order_number
);

SET @order_id = (
    SELECT id
    FROM orders
    WHERE order_number = @order_number
);

INSERT INTO payments (
    order_id,
    portone_payment_id,
    total_amount,
    pg_amount,
    earned_point_amount,
    status,
    paid_at,
    created_at,
    updated_at
)
SELECT
    @order_id,
    @portone_payment_id,
    @total_amount,
    @pg_amount,
    @earned_point_amount,
    'READY',
    NULL,
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM payments
    WHERE order_id = @order_id
       OR portone_payment_id = @portone_payment_id
);

SELECT
    o.id AS order_id,
    p.id AS payment_id,
    o.member_id,
    o.order_number,
    p.portone_payment_id,
    p.status AS payment_status,
    o.status AS order_status,
    p.pg_amount
FROM orders o
JOIN payments p ON p.order_id = o.id
WHERE o.order_number = @order_number;
