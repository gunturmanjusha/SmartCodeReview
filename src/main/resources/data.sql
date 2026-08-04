INSERT INTO customer_orders
    (customer_name, product_name, quantity, unit_price, status, created_at, updated_at)
VALUES
    ('Grace Hopper', 'Mechanical Keyboard', 2, 75.50, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Alan Turing', 'Developer Monitor', 1, 349.99, 'CONFIRMED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Ada Lovelace', 'USB-C Dock', 3, 89.00, 'SHIPPED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
