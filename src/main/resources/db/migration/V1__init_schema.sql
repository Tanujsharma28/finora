CREATE TABLE users (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    account_type VARCHAR(20) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_account_type CHECK (account_type IN ('SAVINGS','CURRENT','CREDIT'))
);

CREATE TABLE transactions (
    id CHAR(36) PRIMARY KEY,
    account_id CHAR(36) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    category VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    merchant VARCHAR(150),
    txn_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_txn_account FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT chk_txn_type CHECK (txn_type IN ('DEBIT','CREDIT')),
    CONSTRAINT chk_txn_status CHECK (status IN ('PENDING','COMPLETED','FLAGGED','REVERSED'))
);

CREATE TABLE fraud_flags (
    id CHAR(36) PRIMARY KEY,
    transaction_id CHAR(36) NOT NULL UNIQUE,
    reason VARCHAR(255) NOT NULL,
    risk_score INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_flag_txn FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT chk_flag_status CHECK (status IN ('OPEN','REVIEWED','DISMISSED')),
    CONSTRAINT chk_risk_score CHECK (risk_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_txn_account_created ON transactions(account_id, created_at);
CREATE INDEX idx_txn_status ON transactions(status);