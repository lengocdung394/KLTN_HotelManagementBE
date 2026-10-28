-- Remove payer bank-account data previously stored on payment_transactions.
-- Run after deploying the application version that no longer maps these columns.
ALTER TABLE payment_transactions
    DROP COLUMN IF EXISTS counter_account_name,
    DROP COLUMN IF EXISTS counter_account_number,
    DROP COLUMN IF EXISTS counter_account_bank_id,
    DROP COLUMN IF EXISTS counter_account_bank_name;
