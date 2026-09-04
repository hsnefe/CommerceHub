-- Demo catalog for the public deployment.
--
-- The product ids deliberately match the rows already seeded by the inventory
-- service in V2__seed_inventory.sql, so every product in the catalog resolves
-- to a real stock record. That seed also gives the demo three distinct states
-- to show without touching the database:
--
--   a1...0001  100 units, threshold 10  -> orderable
--   a1...0002    3 units, threshold  5  -> below the low-stock threshold
--   a1...0003    0 units, threshold  5  -> reservation is rejected
--
-- Category names avoid 'Electronics', 'Accessories' and 'New', which the
-- integration tests create at runtime and categories.name is unique.

INSERT INTO categories (id, name) VALUES
    ('c1000000-0000-4000-8000-000000000001', 'Consumer Electronics'),
    ('c1000000-0000-4000-8000-000000000002', 'Home & Kitchen');

INSERT INTO products (id, category_id, name, description, price) VALUES
    ('a1000000-0000-4000-8000-000000000001',
     'c1000000-0000-4000-8000-000000000001',
     'Wireless Headphones',
     'Over-ear headphones with active noise cancelling and 30 hours of battery life.',
     2499.00),
    ('a1000000-0000-4000-8000-000000000002',
     'c1000000-0000-4000-8000-000000000001',
     'Mechanical Keyboard',
     'Compact 75% layout with hot-swappable tactile switches and per-key backlighting.',
     1899.00),
    ('a1000000-0000-4000-8000-000000000003',
     'c1000000-0000-4000-8000-000000000002',
     'Filter Coffee Machine',
     'Twelve-cup brewer with a thermal carafe and a programmable timer.',
     1299.00);
