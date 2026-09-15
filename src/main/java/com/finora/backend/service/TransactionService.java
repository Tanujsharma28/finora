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
    
     @CacheEvict(value = "accounts", key = "#request.accountId")
    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + request.getAccountId()));

        Transaction txn = Transaction.builder()
                .account(account)
                .amount(request.getAmount())
                .category(request.getCategory() != null ? request.getCategory() : "OTHER")
                .merchant(request.getMerchant())
                .txnType(request.getTxnType())
                .status(TransactionStatus.PENDING)
                .build();

        Transaction saved = transactionRepository.save(txn);

        BigDecimal delta = request.getTxnType() == TransactionType.DEBIT
                ? request.getAmount().negate()
                : request.getAmount();
        accountRepository.adjustBalance(account.getId(), delta);

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

    public List<TransactionResponse> getTransactionsByAccount(String accountId) {
        return transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId, PageRequest.of(0, 50))
                .stream()
                .map(this::toResponse)
                .toList();
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