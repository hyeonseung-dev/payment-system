-- 결제 확정 API 1,000원 로컬 테스트용 SQL
-- PortOne 결제창 테스트 금액을 1,000원으로 낮춰 확인할 때 사용한다.

USE payment_db;

SET @member_id = 1;
SET @product_name = '1000원 결제 테스트 상품';
SET @order_number = 'ORDER-TEST-PAYMENT-1000-005';
SET @portone_payment_id = 'pay_test_1000_005';
SET @total_amount = 1000;
SET @point_amount = 0;
SET @pg_amount = 1000;
SET @earned_point_amount = 10;

INSERT INTO products (
    name,
    price,
    stock_quantity,
    description,
    category,
    status,
    created_at,
    updated_at
)
SELECT
    @product_name,
    @total_amount,
    100,
    'PortOne 결제창 연동 확인용 1000원 상품',
    'ELECTRONICS',
    'FOR_SALE',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM products
    WHERE name = @product_name
);

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
