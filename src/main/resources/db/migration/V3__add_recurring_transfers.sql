CREATE TABLE recurring_transfers (
    id CHAR(36) PRIMARY KEY,
    from_account_id CHAR(36) NOT NULL,
    to_account_id CHAR(36) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    next_run_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_transfer_id CHAR(36),
    description VARCHAR(255),
    failure_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_recurring_from_account FOREIGN KEY (from_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_recurring_to_account FOREIGN KEY (to_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_recurring_last_transfer FOREIGN KEY (last_transfer_id) REFERENCES transfers(id),
    CONSTRAINT chk_recurring_frequency CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY')),
    CONSTRAINT chk_recurring_status CHECK (status IN ('ACTIVE','PAUSED','CANCELLED')),
    CONSTRAINT chk_recurring_accounts CHECK (from_account_id != to_account_id),
    CONSTRAINT chk_recurring_amount CHECK (amount > 0)
);

CREATE INDEX idx_recurring_next_run ON recurring_transfers(next_run_date, status);
CREATE INDEX idx_recurring_from_account ON recurring_transfers(from_account_id);