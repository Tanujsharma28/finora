package com.finora.backend.service;

import com.finora.backend.domain.*;
import com.finora.backend.dto.TransferRequest;
import com.finora.backend.dto.TransferResponse;
import com.finora.backend.repository.AccountRepository;
import com.finora.backend.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final TransferLegService transferLegService;
    private final AuditService auditService;

    public TransferResponse transfer(TransferRequest request) {

        var existing = transferRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("[Transfer] Idempotent replay for key={}, returning existing transfer={}",
                    request.getIdempotencyKey(), existing.get().getId());
            return toResponse(existing.get());
        }

        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Account fromAccount = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + request.getFromAccountId()));
        Account toAccount = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + request.getToAccountId()));

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Insufficient balance in source account");
        }

        Transfer transfer = Transfer.builder()
                .idempotencyKey(request.getIdempotencyKey())
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(request.getAmount())
                .status(TransferStatus.PENDING)
                .build();
        transfer = transferRepository.save(transfer);

        Transaction debitTxn;
        try {
            debitTxn = transferLegService.debitLeg(fromAccount.getId(), request.getAmount());
        } catch (Exception e) {
            log.error("[Transfer] Debit leg failed for transfer={}: {}", transfer.getId(), e.getMessage());
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setFailureReason("Debit failed: " + e.getMessage());
            transferRepository.save(transfer);
            auditService.log(fromAccount.getUser().getId(), "TRANSFER_FAILED", "Transfer", transfer.getId(),
                    "Debit leg failed: " + e.getMessage());
            return toResponse(transfer);
        }

        transfer.setDebitTransaction(debitTxn);
        transferRepository.save(transfer);

        Transaction creditTxn;
        try {
            creditTxn = transferLegService.creditLeg(toAccount.getId(), request.getAmount());
        } catch (Exception e) {
            log.error("[Transfer] Credit leg failed for transfer={}, compensating: {}", transfer.getId(), e.getMessage());
            transferLegService.compensate(fromAccount.getId(), request.getAmount());
            transfer.setStatus(TransferStatus.COMPENSATED);
            transfer.setFailureReason("Credit failed, debit reversed: " + e.getMessage());
            transferRepository.save(transfer);
            auditService.log(fromAccount.getUser().getId(), "TRANSFER_COMPENSATED", "Transfer", transfer.getId(),
                    "Credit leg failed, debit reversed: " + e.getMessage());
            return toResponse(transfer);
        }

        transfer.setCreditTransaction(creditTxn);
        transfer.setStatus(TransferStatus.COMPLETED);
        transferRepository.save(transfer);

        log.info("[Transfer] Completed transfer={} amount={} from={} to={}",
                transfer.getId(), request.getAmount(), fromAccount.getId(), toAccount.getId());

        auditService.log(fromAccount.getUser().getId(), "TRANSFER_COMPLETED", "Transfer", transfer.getId(),
                request.getAmount() + " from " + fromAccount.getId() + " to " + toAccount.getId());

        return toResponse(transfer);
    }

    private TransferResponse toResponse(Transfer transfer) {
        return TransferResponse.builder()
                .id(transfer.getId())
                .fromAccountId(transfer.getFromAccount().getId())
                .toAccountId(transfer.getToAccount().getId())
                .amount(transfer.getAmount())
                .status(transfer.getStatus())
                .failureReason(transfer.getFailureReason())
                .createdAt(transfer.getCreatedAt())
                .build();
    }
}