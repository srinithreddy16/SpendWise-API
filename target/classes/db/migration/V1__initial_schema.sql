-- Initial schema for SpendWise-API
-- Mirrors current JPA entities and relationships

CREATE TABLE users (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    email varchar(255) NOT NULL UNIQUE,
    name varchar(255),
    password_hash varchar(255)
);

CREATE TABLE categories (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    name varchar(255) NOT NULL,
    user_id uuid NOT NULL,
    CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(id) --The user_id in the categories table must match an existing id in the users table.
);

CREATE TABLE expenses (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    user_id uuid NOT NULL,
    category_id uuid NOT NULL,
    amount numeric(19,2) NOT NULL,
    description varchar(1024),
    expense_date date NOT NULL,
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users(id), --The user_id in the expenses table must match an existing id in the users table.
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES categories(id) -- The category_id in the expenses table must match an existing id in the category table
);

CREATE TABLE budgets (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    user_id uuid NOT NULL,
    amount numeric(19,2) NOT NULL,
    year integer NOT NULL,
    month integer,
    CONSTRAINT fk_budgets_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE budget_categories (
    budget_id uuid NOT NULL,
    category_id uuid NOT NULL,
    CONSTRAINT pk_budget_categories PRIMARY KEY (budget_id, category_id),
    CONSTRAINT fk_budget_categories_budget FOREIGN KEY (budget_id) REFERENCES budgets(id),
    CONSTRAINT fk_budget_categories_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE expense_audit_logs (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    expense_id uuid NOT NULL,
    action varchar(50) NOT NULL,
    details text,
    CONSTRAINT fk_expense_audit_logs_expense FOREIGN KEY (expense_id) REFERENCES expenses(id)
);

-- Indexes for expenses (requested)
CREATE INDEX idx_expenses_user_id ON expenses(user_id);
CREATE INDEX idx_expenses_category_id ON expenses(category_id);
CREATE INDEX idx_expenses_created_at ON expenses(created_at);

-- Recommended indexes for other foreign keys
CREATE INDEX idx_categories_user_id ON categories(user_id);
CREATE INDEX idx_budgets_user_id ON budgets(user_id);
CREATE INDEX idx_budget_categories_budget_id ON budget_categories(budget_id);
CREATE INDEX idx_budget_categories_category_id ON budget_categories(category_id);
CREATE INDEX idx_expense_audit_logs_expense_id ON expense_audit_logs(expense_id);

