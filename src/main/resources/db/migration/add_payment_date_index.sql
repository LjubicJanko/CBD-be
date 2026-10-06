-- Add an index on payment.payment_date for the payments report (date-range filter and sort).
--
-- NOTE: this project runs Hibernate with ddl-auto=update, so this index (idx_payment_date) is
-- created automatically from the @Index declared on the Payment entity on startup. This script is
-- the explicit equivalent for environments where schema changes are applied manually. Idempotent
-- on MySQL 8 via the INFORMATION_SCHEMA guard below (CREATE INDEX has no IF NOT EXISTS support on
-- MySQL 8, so a prepared-statement guard is used instead).

SET @idx_exists = (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'payment' AND index_name = 'idx_payment_date'
);
SET @ddl = IF(@idx_exists = 0,
    'CREATE INDEX idx_payment_date ON payment (payment_date)',
    'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
