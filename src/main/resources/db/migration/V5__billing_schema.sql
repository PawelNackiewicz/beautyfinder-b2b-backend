-- Billing & Reports module

-- Salon subscriptions
CREATE TABLE salon_subscriptions (
    id UUID PRIMARY KEY,
    salon_id UUID NOT NULL REFERENCES salons(id),
    plan VARCHAR(20) NOT NULL,
    monthly_fee DECIMAL(10, 2) NOT NULL,
    commission_rate DECIMAL(5, 4) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_salon_subscriptions_salon_status ON salon_subscriptions(salon_id, status);

-- Billing period reports
CREATE TABLE billing_period_reports (
    id UUID PRIMARY KEY,
    salon_id UUID NOT NULL REFERENCES salons(id),
    year INT NOT NULL,
    month INT NOT NULL CHECK (month BETWEEN 1 AND 12),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    total_appointments INT NOT NULL DEFAULT 0,
    completed_appointments INT NOT NULL DEFAULT 0,
    cancelled_appointments INT NOT NULL DEFAULT 0,
    no_show_appointments INT NOT NULL DEFAULT 0,
    marketplace_appointments INT NOT NULL DEFAULT 0,
    direct_appointments INT NOT NULL DEFAULT 0,
    total_revenue DECIMAL(12, 2) NOT NULL DEFAULT 0,
    marketplace_revenue DECIMAL(12, 2) NOT NULL DEFAULT 0,
    total_commission DECIMAL(12, 2) NOT NULL DEFAULT 0,
    subscription_fee DECIMAL(10, 2) NOT NULL DEFAULT 0,
    report_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    generated_at TIMESTAMPTZ,
    invoice_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_billing_report_salon_period UNIQUE (salon_id, year, month)
);

CREATE INDEX idx_billing_reports_status ON billing_period_reports(report_status);

-- Invoices
CREATE TABLE invoices (
    id UUID PRIMARY KEY,
    salon_id UUID NOT NULL REFERENCES salons(id),
    invoice_number VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    net_amount DECIMAL(10, 2) NOT NULL,
    vat_rate DECIMAL(5, 2) NOT NULL,
    vat_amount DECIMAL(10, 2) NOT NULL,
    gross_amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'PLN',
    paid_at TIMESTAMPTZ,
    notes VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_invoice_number UNIQUE (invoice_number)
);

CREATE INDEX idx_invoices_salon_status ON invoices(salon_id, status);

-- Invoice line items
CREATE TABLE invoice_line_items (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    description VARCHAR(500) NOT NULL,
    quantity DECIMAL(10, 2) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    net_amount DECIMAL(10, 2) NOT NULL,
    item_type VARCHAR(30) NOT NULL,
    appointment_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_line_items_invoice ON invoice_line_items(invoice_id);

-- Add FK from billing_period_reports to invoices
ALTER TABLE billing_period_reports
    ADD CONSTRAINT fk_billing_report_invoice
    FOREIGN KEY (invoice_id) REFERENCES invoices(id);

-- Seed: default BASIC subscription for development
INSERT INTO salon_subscriptions (id, salon_id, plan, monthly_fee, commission_rate, valid_from, status)
SELECT
    gen_random_uuid(),
    s.id,
    'BASIC',
    99.00,
    0.1500,
    '2024-01-01',
    'ACTIVE'
FROM salons s
WHERE NOT EXISTS (
    SELECT 1 FROM salon_subscriptions ss WHERE ss.salon_id = s.id AND ss.status = 'ACTIVE'
);
