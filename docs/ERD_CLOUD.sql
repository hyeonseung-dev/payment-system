-- ERDCloud import용 MySQL DDL
-- 기준: 현재 Spring Boot Entity + 결제 시스템 도메인 관계
-- 참고: 포인트 잔액은 별도 point 테이블이 아니라 members.point_balance로 관리한다.

CREATE TABLE members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    point_balance INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_members_email (email),
    UNIQUE KEY uk_members_phone (phone)
);

CREATE TABLE products (
    product_id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    price INT NOT NULL,
    stock_quantity INT NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL COMMENT 'CLOTHES, FOOD, ELECTRONICS',
    status VARCHAR(30) NOT NULL COMMENT 'FOR_SALE, SOLD_OUT, DISCONTINUED',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (product_id)
);

CREATE TABLE carts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_carts_member_id (member_id),
    CONSTRAINT fk_carts_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT UNSIGNED NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_items_cart_product (cart_id, product_id),
    CONSTRAINT fk_cart_items_cart
        FOREIGN KEY (cart_id) REFERENCES carts (id),
    CONSTRAINT fk_cart_items_product
        FOREIGN KEY (product_id) REFERENCES products (product_id)
);

CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    order_number VARCHAR(50) NOT NULL,
    total_amount INT NOT NULL,
    use_point_amount_snapshot INT NOT NULL,
    status VARCHAR(30) NOT NULL COMMENT 'PAYMENT_PENDING, COMPLETED, CANCELED',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_order_number (order_number),
    CONSTRAINT fk_orders_member
        FOREIGN KEY (member_id) REFERENCES members (id)
);

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    cart_item_id BIGINT,
    product_name_snapshot VARCHAR(100) NOT NULL,
    product_price_snapshot INT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id) REFERENCES products (product_id),
    CONSTRAINT fk_order_items_cart_item
        FOREIGN KEY (cart_item_id) REFERENCES cart_items (id)
);

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    portone_payment_id VARCHAR(100) NOT NULL,
    total_amount BIGINT NOT NULL,
    pg_amount BIGINT NOT NULL,
    earned_point_amount BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL COMMENT 'READY, PAID, FAILED, CANCELLED, PARTIAL_REFUNDED, REFUNDED',
    paid_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_order_id (order_id),
    UNIQUE KEY uk_payments_portone_payment_id (portone_payment_id),
    CONSTRAINT fk_payments_order
        FOREIGN KEY (order_id) REFERENCES orders (id)
);

CREATE TABLE point_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL COMMENT 'USE, EARN, USE_CANCEL, EARN_CANCEL',
    amount INT NOT NULL,
    balance_after INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (point_history_id),
    CONSTRAINT fk_point_histories_member
        FOREIGN KEY (member_id) REFERENCES members (id),
    CONSTRAINT fk_point_histories_payment
        FOREIGN KEY (payment_id) REFERENCES payments (id)
);

CREATE TABLE refunds (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    total_refund_amount BIGINT NOT NULL,
    point_refund_amount BIGINT NOT NULL,
    pg_refund_amount BIGINT NOT NULL,
    earned_point_cancel_amount BIGINT NOT NULL,
    earned_point_deduction_amount BIGINT NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL COMMENT 'REQUESTED, COMPLETED, FAILED',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_refunds_payment
        FOREIGN KEY (payment_id) REFERENCES payments (id)
);

CREATE TABLE refund_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    refund_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    point_refund_amount BIGINT NOT NULL,
    pg_refund_amount BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_refund_items_refund
        FOREIGN KEY (refund_id) REFERENCES refunds (id),
    CONSTRAINT fk_refund_items_order_item
        FOREIGN KEY (order_item_id) REFERENCES order_items (id)
);

CREATE TABLE webhook_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_id BIGINT,
    webhook_id VARCHAR(100) NOT NULL,
    portone_payment_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL COMMENT 'RECEIVED, COMPLETED, FAILED, IGNORED',
    payload TEXT NOT NULL,
    failure_reason VARCHAR(1000),
    processed_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_webhook_events_webhook_id (webhook_id),
    CONSTRAINT fk_webhook_events_payment
        FOREIGN KEY (payment_id) REFERENCES payments (id)
);
