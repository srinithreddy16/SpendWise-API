-- Composite indexes to optimize repository queries.
-- user_id, category_id, created_at already have single-column indexes in V1;
-- these composites match actual query patterns (ownership + soft-delete + sort) for better performance.

-- Expenses: composite index for paginated list by user (main GET /expenses).
-- Filters: user_id, deleted. Sorts: expense_date DESC.
-- Without this, DB may use idx_expenses_user_id then filter/sort; composite avoids extra sort.
CREATE INDEX idx_expenses_user_deleted_expense_date
ON expenses(user_id, deleted, expense_date DESC);

-- Expenses: composite index for category-filtered lists.
-- Filters: user_id, category_id, deleted. Sorts: expense_date DESC.
-- Used by findByUser_IdAndCategory_IdAndDeletedIsFalse and sumAmountByUserAndCategoryAndDateRange.
CREATE INDEX idx_expenses_user_category_deleted_expense_date
ON expenses(user_id, category_id, deleted, expense_date DESC);

-- Expenses: index on deleted for filtering when combined with other indexes.
-- Many queries use WHERE user_id = ? AND deleted = false; boolean in composite above suffices.
-- Standalone index useful if DB chooses index-only scans on deleted.
CREATE INDEX idx_expenses_deleted ON expenses(deleted);

-- Budgets: composite index for list-by-user and list-by-year.
-- Filters: user_id, deleted_at. Sorts: year DESC, month DESC.
-- Used by findByUser_IdAndDeletedAtIsNullOrderByYearDescMonthDesc and findByUser_IdAndYearAndDeletedAtIsNull.
CREATE INDEX idx_budgets_user_deleted_year_month
ON budgets(user_id, deleted_at, year DESC, month DESC NULLS LAST);
