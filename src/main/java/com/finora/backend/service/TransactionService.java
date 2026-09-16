package com.finora.backend.service;

import com.finora.backend.domain.*;
import com.finora.backend.dto.CreateTransactionRequest;
import com.finora.backend.dto.TransactionEvent;
import com.finora.backend.dto.TransactionResponse;
import com.finora.backend.kafka.producer.TransactionEventProducer;
import com.finora.backend.repository.AccountRepository;
import com.finora.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionEventProducer transactionEventProducer;
    private final AuditService auditService;

    @CacheEvict(value = "accounts", key = "#request.accountId")
    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        var existing = transactionRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + request.getAccountId()));

        Transaction txn = Transaction.builder()
                .account(account)
                .amount(request.getAmount())
                .category(request.getCategory() != null ? request.getCategory() : "OTHER")
                .merchant(request.getMerchant())
                .txnType(request.getTxnType())
                .status(TransactionStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        Transaction saved = transactionRepository.save(txn);

        BigDecimal delta = request.getTxnType() == TransactionType.DEBIT
                ? request.getAmount().negate()
                : request.getAmount();
        accountRepository.adjustBalance(account.getId(), delta);

        auditService.log(account.getUser().getId(), "TRANSACTION_CREATED", "Transaction", saved.getId(),
                request.getTxnType() + " of " + request.getAmount() + " on account " + account.getId());

        transactionEventProducer.publish(TransactionEvent.builder()
                .transactionId(saved.getId())
                .accountId(account.getId())
                .userId(account.getUser().getId())
                .amount(saved.getAmount())
                .category(saved.getCategory())
                .merchant(saved.getMerchant())
                .txnType(saved.getTxnType())
                .createdAt(saved.getCreatedAt())
                .build());

        return toResponse(saved);
    }

    @CacheEvict(value = "accounts", allEntries = true)
    @Transactional
    public TransactionResponse reverseTransaction(String transactionId) {
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        if (original.getStatus() != TransactionStatus.FLAGGED) {
            throw new IllegalStateException("Only FLAGGED transactions can be reversed");
        }

        TransactionType compensatingType = original.getTxnType() == TransactionType.DEBIT
                ? TransactionType.CREDIT
                : TransactionType.DEBIT;

        BigDecimal delta = compensatingType == TransactionType.CREDIT
                ? original.getAmount()
                : original.getAmount().negate();

        accountRepository.adjustBalance(original.getAccount().getId(), delta);

        Transaction compensating = Transaction.builder()
                .account(original.getAccount())
                .amount(original.getAmount())
                .category("REVERSAL")
                .merchant("Reversal of " + (original.getMerchant() != null ? original.getMerchant() : original.getId()))
                .txnType(compensatingType)
                .status(TransactionStatus.COMPLETED)
                .build();
        transactionRepository.save(compensating);

        // Save account/user IDs before session is cleared
        String accountId = original.getAccount().getId();
        String userId = original.getAccount().getUser().getId();

        transactionRepository.markAsReversed(original.getId());

        auditService.log(userId, "TRANSACTION_REVERSED", "Transaction", original.getId(),
                "Reversed FLAGGED transaction of " + original.getAmount() + " on account " + accountId);

        transactionEventProducer.publish(TransactionEvent.builder()
                .transactionId(original.getId())
                .accountId(accountId)
                .userId(userId)
                .amount(original.getAmount())
                .category(original.getCategory())
                .merchant(original.getMerchant())
                .txnType(original.getTxnType())
                .createdAt(original.getCreatedAt())
                .build());

        original.setStatus(TransactionStatus.REVERSED);
        return toResponse(original);
    }

    public List<TransactionResponse> getTransactionsByAccount(String accountId) {
        return transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(0, 50))
                .stream()
                .map(this::toResponse)
                .toList();
    }
    public String exportTransactionsAsCsv(String accountId) {
    List<Transaction> transactions = transactionRepository
            .findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(0, 1000))
            .getContent();

    StringBuilder csv = new StringBuilder();
    csv.append("Date,Type,Category,Merchant,Amount,Status\n");

    for (Transaction txn : transactions) {
        csv.append(txn.getCreatedAt()).append(",")
           .append(txn.getTxnType()).append(",")
           .append(escapeCsv(txn.getCategory())).append(",")
           .append(escapeCsv(txn.getMerchant() != null ? txn.getMerchant() : "")).append(",")
           .append(txn.getAmount()).append(",")
           .append(txn.getStatus()).append("\n");
    }

    return csv.toString();
}

private String escapeCsv(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
}

    private TransactionResponse toResponse(Transaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .accountId(txn.getAccount().getId())
                .amount(txn.getAmount())
                .category(txn.getCategory())
                .merchant(txn.getMerchant())
                .txnType(txn.getTxnType())
                .status(txn.getStatus())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}