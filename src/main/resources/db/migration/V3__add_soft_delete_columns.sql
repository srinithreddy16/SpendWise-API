-- Soft delete support for expenses and budgets

ALTER TABLE expenses
    ADD COLUMN deleted_at timestamptz;

ALTER TABLE budgets
    ADD COLUMN deleted_at timestamptz;
