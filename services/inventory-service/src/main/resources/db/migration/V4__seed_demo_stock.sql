-- Stock for the demo catalog products the product service seeds in
-- V2__seed_demo_catalog.sql. Ids 0001-0003 already have rows from
-- V2__seed_inventory.sql and are left untouched: the integration tests pin
-- their exact quantities (0001 orderable, 0002 low stock, 0003 out of stock).
--
-- Everything seeded here is comfortably in stock except the electric kettle
-- (0017), which sits under its threshold. Together with the two rows from V2
-- that gives the demo one out-of-stock and two low-stock products out of
-- thirty, which reads as a normal catalog rather than a broken one.

INSERT INTO inventory_items (product_id, available_quantity, low_stock_threshold) VALUES
    -- Consumer Electronics
    ('a1000000-0000-4000-8000-000000000004', 64, 10),
    ('a1000000-0000-4000-8000-000000000005', 38, 8),
    ('a1000000-0000-4000-8000-000000000006', 21, 5),
    ('a1000000-0000-4000-8000-000000000007', 45, 10),
    ('a1000000-0000-4000-8000-000000000008', 120, 20),

    -- Computer Peripherals
    ('a1000000-0000-4000-8000-000000000009', 96, 15),
    ('a1000000-0000-4000-8000-000000000010', 17, 5),
    ('a1000000-0000-4000-8000-000000000011', 42, 8),
    ('a1000000-0000-4000-8000-000000000012', 58, 10),
    ('a1000000-0000-4000-8000-000000000013', 73, 15),

    -- Home & Kitchen
    ('a1000000-0000-4000-8000-000000000014', 29, 5),
    ('a1000000-0000-4000-8000-000000000015', 33, 8),
    ('a1000000-0000-4000-8000-000000000016', 12, 5),
    ('a1000000-0000-4000-8000-000000000017', 4, 10),
    ('a1000000-0000-4000-8000-000000000018', 51, 10),

    -- Sports & Outdoors
    ('a1000000-0000-4000-8000-000000000019', 26, 5),
    ('a1000000-0000-4000-8000-000000000020', 140, 20),
    ('a1000000-0000-4000-8000-000000000021', 88, 15),
    ('a1000000-0000-4000-8000-000000000022', 15, 5),
    ('a1000000-0000-4000-8000-000000000023', 62, 10),
    ('a1000000-0000-4000-8000-000000000024', 31, 8),

    -- Books & Stationery
    ('a1000000-0000-4000-8000-000000000025', 210, 25),
    ('a1000000-0000-4000-8000-000000000026', 19, 5),
    ('a1000000-0000-4000-8000-000000000027', 77, 15),
    ('a1000000-0000-4000-8000-000000000028', 105, 20),
    ('a1000000-0000-4000-8000-000000000029', 24, 5),
    ('a1000000-0000-4000-8000-000000000030', 48, 10);
