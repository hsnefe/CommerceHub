ALTER TABLE inventory_items
    ADD COLUMN reserved_quantity INTEGER NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0);
