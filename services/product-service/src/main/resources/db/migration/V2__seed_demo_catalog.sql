-- Demo catalog for the public deployment: 5 categories, 30 products.
--
-- Product ids match the stock rows seeded by the inventory service, so every
-- product in the catalog resolves to a real inventory record. Ids 0001-0003
-- come from that service's V2__seed_inventory.sql and carry the three states
-- worth demonstrating; V4__seed_demo_stock.sql covers 0004-0030.
--
--   a1...0001  100 units, threshold 10  -> orderable
--   a1...0002    3 units, threshold  5  -> below the low-stock threshold
--   a1...0003    0 units, threshold  5  -> reservation is rejected
--
-- Thirty products puts the catalog over the page size of 20 used by the API
-- and both dashboards, so the paging controls have a second page to reach.
--
-- Category names avoid 'Electronics', 'Accessories' and 'New', which the
-- integration tests create at runtime and categories.name is unique.

INSERT INTO categories (id, name) VALUES
    ('c1000000-0000-4000-8000-000000000001', 'Consumer Electronics'),
    ('c1000000-0000-4000-8000-000000000002', 'Computer Peripherals'),
    ('c1000000-0000-4000-8000-000000000003', 'Home & Kitchen'),
    ('c1000000-0000-4000-8000-000000000004', 'Sports & Outdoors'),
    ('c1000000-0000-4000-8000-000000000005', 'Books & Stationery');

INSERT INTO products (id, category_id, name, description, price) VALUES
    -- Consumer Electronics
    ('a1000000-0000-4000-8000-000000000001', 'c1000000-0000-4000-8000-000000000001',
     'Wireless Headphones', 'Over-ear headphones with active noise cancelling and 30 hours of battery life.', 2499.00),
    ('a1000000-0000-4000-8000-000000000004', 'c1000000-0000-4000-8000-000000000001',
     'Bluetooth Speaker', 'Splash-resistant portable speaker with a passive radiator for deeper bass.', 1249.00),
    ('a1000000-0000-4000-8000-000000000005', 'c1000000-0000-4000-8000-000000000001',
     'Smartwatch', 'Always-on AMOLED display with GPS, heart-rate tracking and a seven-day battery.', 3799.00),
    ('a1000000-0000-4000-8000-000000000006', 'c1000000-0000-4000-8000-000000000001',
     'Action Camera', '4K60 recording with electronic stabilisation and a waterproof housing.', 5499.00),
    ('a1000000-0000-4000-8000-000000000007', 'c1000000-0000-4000-8000-000000000001',
     'E-Reader', 'Six-inch glare-free display with adjustable warm front lighting.', 2899.00),
    ('a1000000-0000-4000-8000-000000000008', 'c1000000-0000-4000-8000-000000000001',
     'Portable Power Bank', '20000 mAh pack with 65 W USB-C output for phones and laptops.', 749.00),

    -- Computer Peripherals
    ('a1000000-0000-4000-8000-000000000002', 'c1000000-0000-4000-8000-000000000002',
     'Mechanical Keyboard', 'Compact 75% layout with hot-swappable tactile switches and per-key backlighting.', 1899.00),
    ('a1000000-0000-4000-8000-000000000009', 'c1000000-0000-4000-8000-000000000002',
     'Wireless Optical Mouse', 'Lightweight ambidextrous shape with a silent scroll wheel.', 649.00),
    ('a1000000-0000-4000-8000-000000000010', 'c1000000-0000-4000-8000-000000000002',
     '27-inch 4K Monitor', 'IPS panel at 144 Hz with USB-C power delivery and a height-adjustable stand.', 8999.00),
    ('a1000000-0000-4000-8000-000000000011', 'c1000000-0000-4000-8000-000000000002',
     'USB-C Docking Station', 'Eleven ports including dual HDMI, gigabit ethernet and 100 W pass-through.', 2199.00),
    ('a1000000-0000-4000-8000-000000000012', 'c1000000-0000-4000-8000-000000000002',
     'Webcam', '1080p60 sensor with a physical privacy shutter and dual noise-cancelling microphones.', 1099.00),
    ('a1000000-0000-4000-8000-000000000013', 'c1000000-0000-4000-8000-000000000002',
     'Laptop Stand', 'Folding aluminium riser that lifts the screen to eye level.', 549.00),

    -- Home & Kitchen
    ('a1000000-0000-4000-8000-000000000003', 'c1000000-0000-4000-8000-000000000003',
     'Filter Coffee Machine', 'Twelve-cup brewer with a thermal carafe and a programmable timer.', 1299.00),
    ('a1000000-0000-4000-8000-000000000014', 'c1000000-0000-4000-8000-000000000003',
     'Espresso Grinder', 'Flat burr grinder with stepless adjustment and a single-dose hopper.', 2699.00),
    ('a1000000-0000-4000-8000-000000000015', 'c1000000-0000-4000-8000-000000000003',
     'Air Fryer', 'Five-litre basket with a digital panel and eight cooking presets.', 3199.00),
    ('a1000000-0000-4000-8000-000000000016', 'c1000000-0000-4000-8000-000000000003',
     'Stand Mixer', 'Tilt-head mixer with a 4.8-litre bowl, dough hook, whisk and flat beater.', 6499.00),
    ('a1000000-0000-4000-8000-000000000017', 'c1000000-0000-4000-8000-000000000003',
     'Electric Kettle', 'Gooseneck kettle with variable temperature control and a keep-warm mode.', 899.00),
    ('a1000000-0000-4000-8000-000000000018', 'c1000000-0000-4000-8000-000000000003',
     'Cast Iron Skillet', 'Pre-seasoned 26 cm pan suitable for induction, oven and open fire.', 1149.00),

    -- Sports & Outdoors
    ('a1000000-0000-4000-8000-000000000019', 'c1000000-0000-4000-8000-000000000004',
     'Trekking Backpack', '45-litre pack with an adjustable harness and a rain cover.', 2299.00),
    ('a1000000-0000-4000-8000-000000000020', 'c1000000-0000-4000-8000-000000000004',
     'Insulated Water Bottle', 'Vacuum-sealed 750 ml bottle that holds temperature for twelve hours.', 449.00),
    ('a1000000-0000-4000-8000-000000000021', 'c1000000-0000-4000-8000-000000000004',
     'Yoga Mat', 'Six-millimetre natural rubber mat with an alignment print.', 699.00),
    ('a1000000-0000-4000-8000-000000000022', 'c1000000-0000-4000-8000-000000000004',
     'Adjustable Dumbbell Set', 'Pair of dial-adjustable dumbbells covering 2 to 24 kg each.', 4899.00),
    ('a1000000-0000-4000-8000-000000000023', 'c1000000-0000-4000-8000-000000000004',
     'Camping Lantern', 'Rechargeable 1000-lumen lantern with a red night mode.', 599.00),
    ('a1000000-0000-4000-8000-000000000024', 'c1000000-0000-4000-8000-000000000004',
     'Trail Running Shoes', 'Grippy lugged outsole with a rock plate and a breathable upper.', 3499.00),

    -- Books & Stationery
    ('a1000000-0000-4000-8000-000000000025', 'c1000000-0000-4000-8000-000000000005',
     'Hardcover Notebook', 'A5 dot-grid notebook on 120 gsm fountain-pen-friendly paper.', 249.00),
    ('a1000000-0000-4000-8000-000000000026', 'c1000000-0000-4000-8000-000000000005',
     'Fountain Pen', 'Medium steel nib with a converter and a machined brass barrel.', 1349.00),
    ('a1000000-0000-4000-8000-000000000027', 'c1000000-0000-4000-8000-000000000005',
     'Desk Organizer', 'Felt-lined oak tray for pens, cables and small tools.', 399.00),
    ('a1000000-0000-4000-8000-000000000028', 'c1000000-0000-4000-8000-000000000005',
     'Drafting Pencil Set', 'Four mechanical pencils from 0.3 to 0.9 mm with a lead gauge.', 529.00),
    ('a1000000-0000-4000-8000-000000000029', 'c1000000-0000-4000-8000-000000000005',
     'Leather Journal Cover', 'Full-grain cover sized for A5 notebooks, with a pen loop.', 899.00),
    ('a1000000-0000-4000-8000-000000000030', 'c1000000-0000-4000-8000-000000000005',
     'Bookend Pair', 'Powder-coated steel bookends with a non-slip base.', 329.00);
