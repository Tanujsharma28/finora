ALTER TABLE transactions ADD COLUMN idempotency_key VARCHAR(100) NULL;
ALTER TABLE transactions ADD CONSTRAINT uq_transactions_idempotency_key UNIQUE (idempotency_key);