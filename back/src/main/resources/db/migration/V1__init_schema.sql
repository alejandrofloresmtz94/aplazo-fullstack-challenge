CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ========================
-- Customers
-- ========================
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    second_last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    credit_line_amount NUMERIC(12,2) NOT NULL CHECK (credit_line_amount > 0),
    available_credit_line_amount NUMERIC(12,2) NOT NULL CHECK (available_credit_line_amount >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ========================
-- Loans
-- ========================
CREATE TABLE loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE','LATE','COMPLETED')),
    commission_amount NUMERIC(12,2) NOT NULL CHECK (commission_amount > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ========================
-- Installments
-- ========================
CREATE TABLE installments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES loans(id) ON DELETE CASCADE,
    amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    scheduled_payment_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('NEXT','PENDING','ERROR'))
);

-- ========================
-- Error Logs
-- ========================
CREATE TABLE error_logs (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL CHECK (code ~ '^APZ[0-9]{6}$'),
    error VARCHAR(100) NOT NULL,
    timestamp BIGINT NOT NULL,
    message TEXT,
    path TEXT
);
