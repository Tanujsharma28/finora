package com.finora.backend.service;

import com.finora.backend.domain.*;
import com.finora.backend.repository.AccountRepository;
import com.finora.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferLegService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction debitLeg(String accountId, BigDecimal amount) {
        int updated = accountRepository.adjustBalance(accountId, amount.negate());
        if (updated == 0) {
            throw new IllegalStateException("Account not found during debit: " + accountId);
        }
        Transaction txn = Transaction.builder()
                .account(accountRepository.getReferenceById(accountId))
                .amount(amount)
                .category("TRANSFER_OUT")
                .txnType(TransactionType.DEBIT)
                .status(TransactionStatus.COMPLETED)
                .build();
        return transactionRepository.save(txn);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Transaction creditLeg(String accountId, BigDecimal amount) {
        int updated = accountRepository.adjustBalance(accountId, amount);
        if (updated == 0) {
            throw new IllegalStateException("Account not found during credit: " + accountId);
        }
        Transaction txn = Transaction.builder()
                .account(accountRepository.getReferenceById(accountId))
                .amount(amount)
                .category("TRANSFER_IN")
                .txnType(TransactionType.CREDIT)
                .status(TransactionStatus.COMPLETED)
                .build();
        return transactionRepository.save(txn);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(String fromAccountId, BigDecimal amount) {
        accountRepository.adjustBalance(fromAccountId, amount);
        Transaction reversal = Transaction.builder()
                .account(accountRepository.getReferenceById(fromAccountId))
                .amount(amount)
                .category("TRANSFER_REVERSAL")
                .txnType(TransactionType.CREDIT)
                .status(TransactionStatus.REVERSED)
                .build();
        transactionRepository.save(reversal);
        log.warn("[Transfer] Compensated debit of {} on account={}", amount, fromAccountId);
    }
}