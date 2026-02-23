-- V8: Services module - extend schema with categories, statuses, and additional fields
-- =====================================================================================

-- Service categories table
CREATE TABLE service_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    salon_id UUID NOT NULL REFERENCES salons(id),
    name VARCHAR(50) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    color_hex VARCHAR(7),
    icon_name VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (salon_id, name)
);
CREATE INDEX idx_service_categories_salon ON service_categories(salon_id);

-- Extend services table
ALTER TABLE services ADD COLUMN IF NOT EXISTS description VARCHAR(1000);
ALTER TABLE services ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);
ALTER TABLE services ADD COLUMN IF NOT EXISTS display_order INT NOT NULL DEFAULT 0;
ALTER TABLE services ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE services ADD COLUMN IF NOT EXISTS is_online_bookable BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE services ADD COLUMN IF NOT EXISTS category_id UUID REFERENCES service_categories(id);

-- Update name column length
ALTER TABLE services ALTER COLUMN name TYPE VARCHAR(100);
ALTER TABLE services ALTER COLUMN category TYPE VARCHAR(50);

-- Add composite index for category queries
CREATE INDEX IF NOT EXISTS idx_services_salon_category ON services(salon_id, category);

-- Extend service_variants table
ALTER TABLE service_variants ADD COLUMN IF NOT EXISTS salon_id UUID;
ALTER TABLE service_variants ADD COLUMN IF NOT EXISTS description VARCHAR(500);
ALTER TABLE service_variants ADD COLUMN IF NOT EXISTS price_max NUMERIC(10, 2);
ALTER TABLE service_variants ADD COLUMN IF NOT EXISTS display_order INT NOT NULL DEFAULT 0;
ALTER TABLE service_variants ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE service_variants ADD COLUMN IF NOT EXISTS is_online_bookable BOOLEAN NOT NULL DEFAULT true;

-- Backfill salon_id from services table
UPDATE service_variants sv
SET salon_id = s.salon_id
FROM services s
WHERE sv.service_id = s.id AND sv.salon_id IS NULL;

-- Now make salon_id NOT NULL
ALTER TABLE service_variants ALTER COLUMN salon_id SET NOT NULL;

-- Add foreign key for salon_id
ALTER TABLE service_variants ADD CONSTRAINT fk_variants_salon FOREIGN KEY (salon_id) REFERENCES salons(id);

-- Update variant name column length
ALTER TABLE service_variants ALTER COLUMN name TYPE VARCHAR(100);

-- Index for variants by service
CREATE INDEX IF NOT EXISTS idx_variants_service ON service_variants(service_id);

-- ==================== SEED DATA ====================

-- Categories for Glamour Studio
INSERT INTO service_categories (id, salon_id, name, display_order, color_hex, icon_name) VALUES
    ('e0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Włosy', 0, '#E91E63', 'scissors'),
    ('e0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'Paznokcie', 1, '#9C27B0', 'nail-polish'),
    ('e0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'Brwi i rzęsy', 2, '#FF9800', 'eye');

-- Update existing services with new fields
UPDATE services SET
    display_order = 0,
    status = 'ACTIVE',
    is_online_bookable = true,
    category_id = 'e0000000-0000-0000-0000-000000000001'
WHERE id = 'd0000000-0000-0000-0000-000000000001';

UPDATE services SET
    display_order = 1,
    status = 'ACTIVE',
    is_online_bookable = true,
    category_id = 'e0000000-0000-0000-0000-000000000001'
WHERE id = 'd0000000-0000-0000-0000-000000000002';

UPDATE services SET
    display_order = 2,
    status = 'ACTIVE',
    is_online_bookable = true,
    category_id = 'e0000000-0000-0000-0000-000000000001'
WHERE id = 'd0000000-0000-0000-0000-000000000003';

UPDATE services SET
    display_order = 3,
    status = 'ACTIVE',
    is_online_bookable = true,
    category_id = 'e0000000-0000-0000-0000-000000000002'
WHERE id = 'd0000000-0000-0000-0000-000000000004';

UPDATE services SET
    display_order = 4,
    status = 'ACTIVE',
    is_online_bookable = true,
    category_id = 'e0000000-0000-0000-0000-000000000002'
WHERE id = 'd0000000-0000-0000-0000-000000000005';

UPDATE services SET
    display_order = 5,
    status = 'ACTIVE',
    is_online_bookable = true,
    category_id = 'e0000000-0000-0000-0000-000000000003'
WHERE id = 'd0000000-0000-0000-0000-000000000006';

-- Update existing variants with new fields (display_order, status)
-- These updates match the seed data variant IDs from V6
UPDATE service_variants SET
    display_order = 0,
    status = 'ACTIVE',
    is_online_bookable = true
WHERE status IS NULL OR status = 'ACTIVE';
