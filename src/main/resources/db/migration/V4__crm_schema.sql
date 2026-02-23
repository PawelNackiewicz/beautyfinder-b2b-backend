-- V4: CRM module schema - Client Cards

-- Alter existing clients table: add new columns for CRM
ALTER TABLE clients ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);
ALTER TABLE clients ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);
ALTER TABLE clients ADD COLUMN IF NOT EXISTS birth_date DATE;
ALTER TABLE clients ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE clients ADD COLUMN IF NOT EXISTS source VARCHAR(50);
ALTER TABLE clients ADD COLUMN IF NOT EXISTS external_id VARCHAR(255);
ALTER TABLE clients ADD COLUMN IF NOT EXISTS preferred_employee_id UUID;
ALTER TABLE clients ADD COLUMN IF NOT EXISTS notes VARCHAR(2000);
ALTER TABLE clients ADD COLUMN IF NOT EXISTS total_visits INT NOT NULL DEFAULT 0;
ALTER TABLE clients ADD COLUMN IF NOT EXISTS total_spent DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE clients ADD COLUMN IF NOT EXISTS last_visit_at TIMESTAMPTZ;

-- Migrate existing data: split name into firstName/lastName
UPDATE clients SET first_name = split_part(name, ' ', 1),
                   last_name = COALESCE(NULLIF(substring(name from position(' ' in name) + 1), ''), split_part(name, ' ', 1))
WHERE first_name IS NULL;

-- Make first_name and last_name not null
ALTER TABLE clients ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE clients ALTER COLUMN last_name SET NOT NULL;

-- Drop old name column
ALTER TABLE clients DROP COLUMN IF EXISTS name;

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_clients_salon ON clients(salon_id);
CREATE INDEX IF NOT EXISTS idx_clients_phone ON clients(salon_id, phone);
CREATE INDEX IF NOT EXISTS idx_clients_email ON clients(salon_id, email);

-- Client Sensitive Data
CREATE TABLE client_sensitive_data (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL UNIQUE REFERENCES clients(id),
    encrypted_data VARCHAR(4000) NOT NULL,
    data_hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- GDPR Consents (append-only log)
CREATE TABLE gdpr_consents (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES clients(id),
    salon_id UUID NOT NULL REFERENCES salons(id),
    consent_type VARCHAR(30) NOT NULL,
    granted BOOLEAN NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL,
    ip_address VARCHAR(45),
    consent_version VARCHAR(10) NOT NULL,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_gdpr_consents_client ON gdpr_consents(client_id, salon_id);
CREATE INDEX idx_gdpr_consents_type ON gdpr_consents(client_id, consent_type, granted);

-- Blacklist Entries
CREATE TABLE blacklist_entries (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES clients(id),
    salon_id UUID NOT NULL REFERENCES salons(id),
    reason VARCHAR(500) NOT NULL,
    created_by UUID NOT NULL,
    removed_at TIMESTAMPTZ,
    removed_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_blacklist_client_active ON blacklist_entries(client_id, salon_id) WHERE removed_at IS NULL;

-- Loyalty Balances
CREATE TABLE loyalty_balances (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES clients(id),
    salon_id UUID NOT NULL REFERENCES salons(id),
    points INT NOT NULL DEFAULT 0 CHECK (points >= 0),
    total_earned INT NOT NULL DEFAULT 0,
    total_redeemed INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (client_id, salon_id)
);

-- Loyalty Transactions
CREATE TABLE loyalty_transactions (
    id UUID PRIMARY KEY,
    client_id UUID NOT NULL REFERENCES clients(id),
    salon_id UUID NOT NULL REFERENCES salons(id),
    points INT NOT NULL,
    type VARCHAR(30) NOT NULL,
    appointment_id UUID,
    note VARCHAR(200),
    balance_after INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_loyalty_transactions_client ON loyalty_transactions(client_id, salon_id, created_at DESC);
