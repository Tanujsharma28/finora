package com.finora.backend.service;

import com.finora.backend.domain.Account;
import com.finora.backend.domain.RecurringTransfer;
import com.finora.backend.domain.RecurringTransferStatus;
import com.finora.backend.dto.recurring.CreateRecurringTransferRequest;
import com.finora.backend.dto.recurring.RecurringTransferResponse;
import com.finora.backend.exception.ResourceNotFoundException;
import com.finora.backend.repository.AccountRepository;
import com.finora.backend.repository.RecurringTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringTransferService {

    private final RecurringTransferRepository recurringTransferRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public RecurringTransferResponse create(CreateRecurringTransferRequest request) {

        if (request.fromAccountId().equals(request.toAccountId())) {
            throw new IllegalArgumentException("Source and destination account cannot be the same");
        }

        Account fromAccount = accountRepository.findById(request.fromAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Source account not found: " + request.fromAccountId()));

        Account toAccount = accountRepository.findById(request.toAccountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Destination account not found: " + request.toAccountId()));

        RecurringTransfer recurringTransfer = RecurringTransfer.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(request.amount())
                .frequency(request.frequency())
                .nextRunDate(request.startDate())
                .status(RecurringTransferStatus.ACTIVE)
                .description(request.description())
                .build();

        RecurringTransfer saved = recurringTransferRepository.save(recurringTransfer);
        return toResponse(saved);
    }

    @Transactional
    public RecurringTransferResponse pause(String id) {
        RecurringTransfer recurringTransfer = findByIdOrThrow(id);

        if (recurringTransfer.getStatus() == RecurringTransferStatus.CANCELLED) {
            throw new IllegalStateException("Cannot pause a cancelled recurring transfer");
        }

        recurringTransfer.setStatus(RecurringTransferStatus.PAUSED);
        return toResponse(recurringTransferRepository.save(recurringTransfer));
    }

    @Transactional
    public RecurringTransferResponse resume(String id) {
        RecurringTransfer recurringTransfer = findByIdOrThrow(id);

        if (recurringTransfer.getStatus() == RecurringTransferStatus.CANCELLED) {
            throw new IllegalStateException("Cannot resume a cancelled recurring transfer");
        }

        recurringTransfer.setStatus(RecurringTransferStatus.ACTIVE);
        return toResponse(recurringTransferRepository.save(recurringTransfer));
    }

    @Transactional
    public RecurringTransferResponse cancel(String id) {
        RecurringTransfer recurringTransfer = findByIdOrThrow(id);
        recurringTransfer.setStatus(RecurringTransferStatus.CANCELLED);
        return toResponse(recurringTransferRepository.save(recurringTransfer));
    }

    @Transactional(readOnly = true)
    public List<RecurringTransferResponse> getByAccount(String accountId) {
        return recurringTransferRepository.findByFromAccountId(accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private RecurringTransfer findByIdOrThrow(String id) {
        return recurringTransferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Recurring transfer not found: " + id));
    }

    private RecurringTransferResponse toResponse(RecurringTransfer recurringTransfer) {
        return new RecurringTransferResponse(
                recurringTransfer.getId(),
                recurringTransfer.getFromAccount().getId(),
                recurringTransfer.getToAccount().getId(),
                recurringTransfer.getAmount(),
                recurringTransfer.getFrequency(),
                recurringTransfer.getStatus(),
                recurringTransfer.getNextRunDate(),
                recurringTransfer.getDescription(),
                recurringTransfer.getCreatedAt()
        );
    }
}
