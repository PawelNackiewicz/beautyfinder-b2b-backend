-- Add new columns to appointments table
ALTER TABLE appointments
    ADD COLUMN end_at TIMESTAMPTZ,
    ADD COLUMN commission_value NUMERIC(10, 2),
    ADD COLUMN notes VARCHAR(500),
    ADD COLUMN cancellation_reason VARCHAR(500);

-- Update source column: convert from free-text to enum-like VARCHAR
UPDATE appointments SET source = 'DIRECT' WHERE source IS NULL;
ALTER TABLE appointments ALTER COLUMN source SET NOT NULL;
ALTER TABLE appointments ALTER COLUMN source SET DEFAULT 'DIRECT';

-- Backfill end_at for existing appointments (default 30 min duration)
UPDATE appointments SET end_at = start_at + INTERVAL '30 minutes' WHERE end_at IS NULL;
ALTER TABLE appointments ALTER COLUMN end_at SET NOT NULL;

-- Add CONFIRMED and IN_PROGRESS to status (already VARCHAR, no schema change needed)

-- Better indexes for appointments
DROP INDEX IF EXISTS idx_appointments_salon_id;
CREATE INDEX idx_appointments_salon_date ON appointments(salon_id, start_at);
CREATE INDEX idx_appointments_employee ON appointments(employee_id, start_at, status);

-- Weekly schedules table
CREATE TABLE weekly_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    salon_id UUID NOT NULL REFERENCES salons(id),
    day_of_week VARCHAR(10) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_weekly_schedule_employee_day UNIQUE (employee_id, day_of_week)
);

-- Schedule exceptions table (vacations, blocks)
CREATE TABLE schedule_exceptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    salon_id UUID NOT NULL REFERENCES salons(id),
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    reason VARCHAR(255),
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_schedule_exceptions_employee ON schedule_exceptions(employee_id, start_at, end_at);
