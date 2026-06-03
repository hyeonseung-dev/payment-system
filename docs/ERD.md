
# docs/ERD.md

# ERD

## Tables

- members
- points
- point_histories
- products
- carts
- cart_items
- orders
- order_items
- payments
- refunds
- refund_items
- webhook_events

## Key Relations

```text
members 1 : 1 points
members 1 : 1 carts
carts 1 : N cart_items
products 1 : N cart_items

members 1 : N orders
orders 1 : N order_items
products 1 : N order_items

orders 1 : 1 payments
payments 1 : N refunds
refunds 1 : N refund_items
order_items 1 : N refund_items

points 1 : N point_histories
payments 1 : N point_histories

payments 1 : N webhook_events
````

## Important Constraints

```text
members.email UNIQUE
carts.member_id UNIQUE
cart_items(cart_id, product_id) UNIQUE
orders.order_number UNIQUE
payments.order_id UNIQUE
payments.portone_payment_id UNIQUE
webhook_events.webhook_id UNIQUE
```

## Snapshot Columns

```text
order_items.product_name_snapshot
order_items.product_price_snapshot
point_histories.balance_after
```

## Webhook Event Columns

```text
webhook_events.payload TEXT
webhook_events.failure_reason VARCHAR(1000)
```
