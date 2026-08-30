-- Add per-tenant text color tiers: primary/muted/subtle. Follow-up to
-- add_tenant_theme_colors.sql (accent + background), same semantics.
--
-- All nullable, no default and no backfill: existing tenants stay NULL, which
-- the frontend reads as "use the built-in default palette". Values are 6-digit
-- uppercase hex strings (e.g. #D4FF00); the FE derives every other shade.
--
-- NOTE: this project runs Hibernate with ddl-auto=update, so these columns are
-- created automatically from the Tenant entity on startup. This script is the
-- explicit equivalent for environments where schema changes are applied manually
-- (e.g. a production DB where ddl-auto is disabled). Idempotent on MySQL 8 via
-- the INFORMATION_SCHEMA guard below (plain ADD COLUMN has no IF NOT EXISTS
-- support on MySQL 8, so a prepared-statement guard is used instead).

SET @col_exists = (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tenant' AND column_name = 'text_color'
);
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE tenant
        ADD COLUMN text_color        VARCHAR(7) NULL DEFAULT NULL,
        ADD COLUMN muted_text_color  VARCHAR(7) NULL DEFAULT NULL,
        ADD COLUMN subtle_text_color VARCHAR(7) NULL DEFAULT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
