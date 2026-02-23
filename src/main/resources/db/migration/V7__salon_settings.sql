-- V7: Salon Settings module
-- Extend salons table with new columns and create settings tables

-- ==================== EXTEND SALONS TABLE ====================

ALTER TABLE salons ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE salons ADD COLUMN logo_url VARCHAR(500);
ALTER TABLE salons ADD COLUMN cover_image_url VARCHAR(500);
ALTER TABLE salons ADD COLUMN description VARCHAR(2000);
ALTER TABLE salons ADD COLUMN website_url VARCHAR(500);
ALTER TABLE salons ADD COLUMN phone VARCHAR(50);
ALTER TABLE salons ADD COLUMN email VARCHAR(255);

-- Address
ALTER TABLE salons ADD COLUMN street VARCHAR(200);
ALTER TABLE salons ADD COLUMN city VARCHAR(100);
ALTER TABLE salons ADD COLUMN postal_code VARCHAR(10);
ALTER TABLE salons ADD COLUMN country VARCHAR(10) NOT NULL DEFAULT 'PL';
ALTER TABLE salons ADD COLUMN latitude NUMERIC(10, 7);
ALTER TABLE salons ADD COLUMN longitude NUMERIC(10, 7);

-- Operational settings
ALTER TABLE salons ADD COLUMN timezone VARCHAR(50) NOT NULL DEFAULT 'Europe/Warsaw';
ALTER TABLE salons ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'PLN';
ALTER TABLE salons ADD COLUMN booking_window_days INT NOT NULL DEFAULT 60;
ALTER TABLE salons ADD COLUMN slot_interval_minutes INT NOT NULL DEFAULT 15;
ALTER TABLE salons ADD COLUMN default_appointment_buffer_minutes INT NOT NULL DEFAULT 0;
ALTER TABLE salons ADD COLUMN max_advance_booking_days INT NOT NULL DEFAULT 90;

-- Invoicing
ALTER TABLE salons ADD COLUMN invoicing_name VARCHAR(200);
ALTER TABLE salons ADD COLUMN tax_id VARCHAR(20);
ALTER TABLE salons ADD COLUMN invoicing_street VARCHAR(200);
ALTER TABLE salons ADD COLUMN invoicing_city VARCHAR(100);
ALTER TABLE salons ADD COLUMN invoicing_postal_code VARCHAR(10);
ALTER TABLE salons ADD COLUMN bank_account_number VARCHAR(34);
ALTER TABLE salons ADD COLUMN invoice_footer_notes VARCHAR(500);

-- Loyalty program
ALTER TABLE salons ADD COLUMN loyalty_enabled BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE salons ADD COLUMN points_per_visit INT;
ALTER TABLE salons ADD COLUMN points_per_currency_unit INT;
ALTER TABLE salons ADD COLUMN points_redemption_rate NUMERIC(10, 2);
ALTER TABLE salons ADD COLUMN loyalty_points_expire_days INT;

-- Booking policy
ALTER TABLE salons ADD COLUMN require_client_phone BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE salons ADD COLUMN allow_online_booking BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE salons ADD COLUMN auto_confirm_appointments BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE salons ADD COLUMN online_booking_message VARCHAR(500);

-- Indexes on salons
CREATE INDEX idx_salons_status ON salons(status);

-- ==================== SALON OPENING HOURS ====================

CREATE TABLE salon_opening_hours (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    salon_id UUID NOT NULL REFERENCES salons(id),
    day_of_week VARCHAR(20) NOT NULL,
    is_open BOOLEAN NOT NULL DEFAULT true,
    open_time TIME,
    close_time TIME,
    break_start TIME,
    break_end TIME,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_salon_opening_hours_salon_day UNIQUE (salon_id, day_of_week)
);
CREATE INDEX idx_salon_opening_hours_salon_id ON salon_opening_hours(salon_id);

-- ==================== SALON NOTIFICATION SETTINGS ====================

CREATE TABLE salon_notification_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    salon_id UUID NOT NULL REFERENCES salons(id),
    appointment_reminder_enabled BOOLEAN NOT NULL DEFAULT true,
    appointment_reminder_hours_before INT NOT NULL DEFAULT 24,
    appointment_confirmation_enabled BOOLEAN NOT NULL DEFAULT true,
    cancellation_notification_enabled BOOLEAN NOT NULL DEFAULT true,
    marketing_emails_enabled BOOLEAN NOT NULL DEFAULT false,
    sms_notifications_enabled BOOLEAN NOT NULL DEFAULT false,
    email_notifications_enabled BOOLEAN NOT NULL DEFAULT true,
    notification_email VARCHAR(255),
    notification_phone VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_salon_notification_settings_salon UNIQUE (salon_id)
);

-- ==================== SALON INTEGRATION SETTINGS ====================

CREATE TABLE salon_integration_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    salon_id UUID NOT NULL REFERENCES salons(id),
    google_calendar_enabled BOOLEAN NOT NULL DEFAULT false,
    google_calendar_token TEXT,
    facebook_pixel_id VARCHAR(50),
    google_analytics_id VARCHAR(50),
    webhook_url VARCHAR(500),
    webhook_secret TEXT,
    marketplace_enabled BOOLEAN NOT NULL DEFAULT true,
    marketplace_profile_visible BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_salon_integration_settings_salon UNIQUE (salon_id)
);

-- ==================== SEED DATA (DEV) ====================

-- Update existing salons with new fields
UPDATE salons SET
    status = 'ACTIVE',
    timezone = 'Europe/Warsaw',
    currency = 'PLN',
    phone = '+48221234567',
    email = 'kontakt@glamourstudio.pl',
    street = 'ul. Marszałkowska 10',
    city = 'Warszawa',
    postal_code = '00-001',
    booking_window_days = 60,
    slot_interval_minutes = 15
WHERE id = 'a0000000-0000-0000-0000-000000000001';

UPDATE salons SET
    status = 'ACTIVE',
    timezone = 'Europe/Warsaw',
    currency = 'PLN',
    phone = '+48121234567',
    email = 'kontakt@beautylab.pl',
    street = 'ul. Floriańska 15',
    city = 'Kraków',
    postal_code = '31-019',
    booking_window_days = 60,
    slot_interval_minutes = 15
WHERE id = 'a0000000-0000-0000-0000-000000000002';

UPDATE salons SET
    status = 'ACTIVE',
    timezone = 'Europe/Warsaw',
    currency = 'PLN',
    phone = '+48611234567',
    email = 'kontakt@salonperla.pl',
    street = 'ul. Półwiejska 20',
    city = 'Poznań',
    postal_code = '61-888',
    booking_window_days = 60,
    slot_interval_minutes = 15
WHERE id = 'a0000000-0000-0000-0000-000000000003';

-- Opening hours for Glamour Studio (Mon-Fri 9-18, Sat 9-14, Sun closed)
INSERT INTO salon_opening_hours (salon_id, day_of_week, is_open, open_time, close_time) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'MONDAY',    true,  '09:00', '18:00'),
    ('a0000000-0000-0000-0000-000000000001', 'TUESDAY',   true,  '09:00', '18:00'),
    ('a0000000-0000-0000-0000-000000000001', 'WEDNESDAY', true,  '09:00', '18:00'),
    ('a0000000-0000-0000-0000-000000000001', 'THURSDAY',  true,  '09:00', '18:00'),
    ('a0000000-0000-0000-0000-000000000001', 'FRIDAY',    true,  '09:00', '18:00'),
    ('a0000000-0000-0000-0000-000000000001', 'SATURDAY',  true,  '09:00', '14:00'),
    ('a0000000-0000-0000-0000-000000000001', 'SUNDAY',    false, NULL,    NULL);

-- Opening hours for Beauty Lab Kraków
INSERT INTO salon_opening_hours (salon_id, day_of_week, is_open, open_time, close_time) VALUES
    ('a0000000-0000-0000-0000-000000000002', 'MONDAY',    true,  '08:00', '20:00'),
    ('a0000000-0000-0000-0000-000000000002', 'TUESDAY',   true,  '08:00', '20:00'),
    ('a0000000-0000-0000-0000-000000000002', 'WEDNESDAY', true,  '08:00', '20:00'),
    ('a0000000-0000-0000-0000-000000000002', 'THURSDAY',  true,  '08:00', '20:00'),
    ('a0000000-0000-0000-0000-000000000002', 'FRIDAY',    true,  '08:00', '20:00'),
    ('a0000000-0000-0000-0000-000000000002', 'SATURDAY',  true,  '10:00', '16:00'),
    ('a0000000-0000-0000-0000-000000000002', 'SUNDAY',    false, NULL,    NULL);

-- Notification settings for Glamour Studio
INSERT INTO salon_notification_settings (salon_id, appointment_reminder_enabled, appointment_reminder_hours_before,
    appointment_confirmation_enabled, cancellation_notification_enabled, email_notifications_enabled,
    notification_email) VALUES
    ('a0000000-0000-0000-0000-000000000001', true, 24, true, true, true, 'kontakt@glamourstudio.pl');
