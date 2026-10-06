-- Backfill stored order balances that went stale when an order's price was edited.
--
-- Before the fix in OrderServiceImpl.updateOrder, editing salePrice / acquisitionCost left
-- amount_left_to_pay, amount_left_to_pay_with_tax and price_difference at their old values.
-- updateOrder now recomputes them on every call; this script is the manual equivalent for
-- existing rows (Hibernate ddl-auto=update only manages schema, never data).
--
-- Formulas (same as OrderRecord.addPayment / editPayment / deletePayment):
--   amount_left_to_pay          = sale_price          - COALESCE(amount_paid, 0)
--   amount_left_to_pay_with_tax = sale_price_with_tax - COALESCE(amount_paid, 0)
--   price_difference            = sale_price          - COALESCE(acquisition_cost, 0)
--
-- Scope: every row with sale_price IS NOT NULL, soft-deleted rows included (raw SQL ignores
-- @SQLRestriction). Rows with a NULL sale_price_with_tax keep amount_left_to_pay_with_tax as is
-- (NULL arithmetic would otherwise overwrite it with NULL). amount_paid is never modified.
--
-- Idempotent: the values are pure functions of the other columns, so re-running changes nothing.
--
-- Optional read-only pre-check (not executed): orders whose amount_paid disagrees with the sum of
-- their payment rows. This script does not repair those.
--   SELECT o.id, o.amount_paid, COALESCE(SUM(p.amount), 0) AS payments_sum
--   FROM order_record o
--   LEFT JOIN payment p ON p.order_id = o.id
--   GROUP BY o.id, o.amount_paid
--   HAVING COALESCE(o.amount_paid, 0) <> COALESCE(SUM(p.amount), 0);

UPDATE order_record
SET amount_left_to_pay          = sale_price - COALESCE(amount_paid, 0),
    amount_left_to_pay_with_tax = CASE
        WHEN sale_price_with_tax IS NULL THEN amount_left_to_pay_with_tax
        ELSE sale_price_with_tax - COALESCE(amount_paid, 0)
    END,
    price_difference            = sale_price - COALESCE(acquisition_cost, 0)
WHERE sale_price IS NOT NULL;
