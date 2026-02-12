-- Add deleted boolean column to expenses table for efficient soft delete queries
-- This complements deleted_at timestamp (which tracks when deletion occurred)
-- Boolean is preferred for queries due to better indexing and simpler conditions

ALTER TABLE expenses
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN expenses.deleted IS 'Soft delete flag. When true, expense is deleted but preserved for audit trail. Defaults to false.';

-- Migrate existing soft-deleted records (where deleted_at IS NOT NULL) to deleted = true
UPDATE expenses
SET deleted = TRUE
WHERE deleted_at IS NOT NULL;
