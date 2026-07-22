-- Restore device status for soft-delete and recovery.
-- Skip this migration if the status column already exists.

ALTER TABLE device
    ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' AFTER ip_address;
