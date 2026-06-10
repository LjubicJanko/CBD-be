-- Add per-tenant theme colors: accent (brand) + background.
--
-- Both nullable, no default and no backfill: existing tenants stay NULL, which
-- the frontend reads as "use the built-in default palette". Values are 6-digit
-- uppercase hex strings (e.g. #D4FF00); the FE derives every other shade.
--
-- NOTE: this project runs Hibernate with ddl-auto=update, so these columns are
-- created automatically from the Tenant entity on startup. This script is the
-- explicit equivalent for environments where schema changes are applied manually
-- (e.g. a production DB where ddl-auto is disabled). Idempotent on MySQL 8.

ALTER TABLE tenant
    ADD COLUMN accent_color     VARCHAR(7) NULL DEFAULT NULL,
    ADD COLUMN background_color VARCHAR(7) NULL DEFAULT NULL;
