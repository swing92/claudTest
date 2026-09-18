CREATE TABLE account (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('CASH', 'BANK', 'CARD')),
    currency VARCHAR(3) NOT NULL DEFAULT 'KRW',
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_account_user_id ON account (user_id);

CREATE TABLE transaction_category (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    color_hex VARCHAR(7),
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_transaction_category_user_id ON transaction_category (user_id);

CREATE TABLE transaction (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL REFERENCES account (id),
    category_id BIGINT NOT NULL REFERENCES transaction_category (id),
    amount NUMERIC(14, 2) NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    memo VARCHAR(500),
    occurred_at DATE NOT NULL,
    source VARCHAR(10) NOT NULL DEFAULT 'MANUAL' CHECK (source IN ('MANUAL', 'SYNCED')),
    external_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_transaction_user_id ON transaction (user_id);
CREATE INDEX idx_transaction_account_id ON transaction (account_id);
CREATE INDEX idx_transaction_category_id ON transaction (category_id);
CREATE INDEX idx_transaction_occurred_at ON transaction (occurred_at);

-- Two different accounts (e.g. a bank feed and a card feed) can coincidentally reuse the same
-- external_id, so uniqueness is scoped to (account_id, external_id), not external_id alone.
-- Partial index (WHERE external_id IS NOT NULL) so manual transactions, which have no external_id,
-- never collide with each other.
CREATE UNIQUE INDEX uq_transaction_account_external_id
    ON transaction (account_id, external_id)
    WHERE external_id IS NOT NULL;
