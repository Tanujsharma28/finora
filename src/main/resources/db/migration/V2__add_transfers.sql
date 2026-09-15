CREATE TABLE transfers (
    id CHAR(36) PRIMARY KEY,
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    from_account_id CHAR(36) NOT NULL,
    to_account_id CHAR(36) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    debit_transaction_id CHAR(36),
    credit_transaction_id CHAR(36),
    failure_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_transfer_from_account FOREIGN KEY (from_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transfer_to_account FOREIGN KEY (to_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transfer_debit_txn FOREIGN KEY (debit_transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_transfer_credit_txn FOREIGN KEY (credit_transaction_id) REFERENCES transactions(id),
    CONSTRAINT chk_transfer_status CHECK (status IN ('PENDING','COMPLETED','FAILED','COMPENSATED')),
    CONSTRAINT chk_transfer_accounts CHECK (from_account_id != to_account_id),
    CONSTRAINT chk_transfer_amount CHECK (amount > 0)
);

CREATE INDEX idx_transfer_from_account ON transfers(from_account_id);
CREATE INDEX idx_transfer_to_account ON transfers(to_account_id);
CREATE INDEX idx_transfer_idempotency ON transfers(idempotency_key);