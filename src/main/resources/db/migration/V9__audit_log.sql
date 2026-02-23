-- V9: Audit Log module
-- ====================

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    salon_id UUID NOT NULL REFERENCES salons(id),
    actor_id UUID,
    actor_email VARCHAR(255),
    actor_role VARCHAR(50),
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id UUID,
    resource_description VARCHAR(500),
    metadata JSONB,
    changed_fields JSONB,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Primary query index: salon + time for paginated listing
CREATE INDEX idx_audit_salon_time ON audit_logs(salon_id, created_at DESC);

-- Resource history queries
CREATE INDEX idx_audit_resource ON audit_logs(resource_type, resource_id);

-- Actor history queries
CREATE INDEX idx_audit_actor ON audit_logs(actor_id);

-- Action-based filtering
CREATE INDEX idx_audit_action ON audit_logs(action);

-- Partial index for sensitive data access (GDPR compliance)
CREATE INDEX idx_audit_sensitive ON audit_logs(resource_type, resource_id)
    WHERE action IN ('CLIENT_SENSITIVE_DATA_ACCESSED', 'CLIENT_SENSITIVE_DATA_UPDATED');

-- NOTE: When audit_logs exceeds 10M rows, consider partitioning by created_at:
-- ALTER TABLE audit_logs RENAME TO audit_logs_old;
-- CREATE TABLE audit_logs (LIKE audit_logs_old INCLUDING ALL) PARTITION BY RANGE (created_at);
-- CREATE TABLE audit_logs_2024 PARTITION OF audit_logs FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
-- etc.

-- Retention policies per salon
CREATE TABLE audit_retention_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    salon_id UUID NOT NULL REFERENCES salons(id) UNIQUE,
    retention_days INT NOT NULL DEFAULT 365,
    sensitive_data_retention_days INT NOT NULL DEFAULT 1825,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed: default retention policy for existing salons
INSERT INTO audit_retention_policies (salon_id, retention_days, sensitive_data_retention_days)
SELECT id, 365, 1825 FROM salons
ON CONFLICT (salon_id) DO NOTHING;
