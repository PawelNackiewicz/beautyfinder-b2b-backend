-- Add new columns to employees table
ALTER TABLE employees
    ADD COLUMN phone VARCHAR(50),
    ADD COLUMN avatar_url VARCHAR(500),
    ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN color VARCHAR(7);

-- Unique constraint: one active employee per user per salon
CREATE UNIQUE INDEX uq_employees_user_salon ON employees(user_id, salon_id) WHERE status != 'DELETED';

-- Index for salon + status queries
CREATE INDEX idx_employees_salon_status ON employees(salon_id, status);

-- Employee-Service mapping table (for @ElementCollection)
CREATE TABLE employee_services (
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES services(id),
    PRIMARY KEY (employee_id, service_id)
);

-- Add is_working_day to weekly_schedules
ALTER TABLE weekly_schedules
    ADD COLUMN is_working_day BOOLEAN NOT NULL DEFAULT true;

-- Add new schedule exception types (SICK_LEAVE, PERSONAL) - already VARCHAR, no schema change needed
-- Update existing 'BLOCKED' entries if any - they remain valid
