package com.finora.backend.service;

import com.finora.backend.domain.RecurringTransfer;
import com.finora.backend.domain.RecurringTransferStatus;
import com.finora.backend.dto.TransferRequest;
import com.finora.backend.dto.TransferResponse;
import com.finora.backend.repository.RecurringTransferRepository;
import com.finora.backend.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransferItemProcessor {

    private static final int MAX_CONSECUTIVE_FAILURES = 3;

    private final RecurringTransferRepository recurringTransferRepository;
    private final TransferService transferService;
    private final TransferRepository transferRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(String recurringTransferId, LocalDate today) {

        RecurringTransfer recurringTransfer = recurringTransferRepository.findById(recurringTransferId)
                .orElse(null);
        if (recurringTransfer == null) {
            log.warn("[RecurringScheduler] Recurring transfer {} not found, skipping", recurringTransferId);
            return;
        }

        String idempotencyKey = "recurring-" + recurringTransfer.getId() + "-" + today;

               TransferRequest request = new TransferRequest();
        request.setFromAccountId(recurringTransfer.getFromAccount().getId());
        request.setToAccountId(recurringTransfer.getToAccount().getId());
        request.setAmount(recurringTransfer.getAmount());
        request.setIdempotencyKey(idempotencyKey);

        try {
            TransferResponse response = transferService.transfer(request);

            if (response.getId() != null) {
                recurringTransfer.setLastTransfer(transferRepository.getReferenceById(response.getId()));
            }

            boolean succeeded = response.getStatus() != null
                    && response.getStatus().name().equals("COMPLETED");

            if (succeeded) {
                recurringTransfer.setFailureCount(0);
            } else {
                handleFailure(recurringTransfer, "underlying transfer status=" + response.getStatus());
            }

        } catch (Exception e) {
            log.error("[RecurringScheduler] Recurring transfer {} threw exception: {}",
                    recurringTransfer.getId(), e.getMessage());
            handleFailure(recurringTransfer, e.getMessage());
        }

        recurringTransfer.setNextRunDate(advance(today, recurringTransfer.getFrequency()));
        recurringTransferRepository.save(recurringTransfer);
    }

    private void handleFailure(RecurringTransfer recurringTransfer, String reason) {
        int failures = recurringTransfer.getFailureCount() == null ? 0 : recurringTransfer.getFailureCount();
        failures++;
        recurringTransfer.setFailureCount(failures);

        log.warn("[RecurringScheduler] Recurring transfer {} failed (attempt {}/{}): {}",
                recurringTransfer.getId(), failures, MAX_CONSECUTIVE_FAILURES, reason);

        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            recurringTransfer.setStatus(RecurringTransferStatus.PAUSED);
            log.warn("[RecurringScheduler] Recurring transfer {} auto-paused after {} consecutive failures",
                    recurringTransfer.getId(), failures);
        }
    }

    private LocalDate advance(LocalDate date, com.finora.backend.domain.RecurringFrequency frequency) {
        return switch (frequency) {
            case DAILY -> date.plusDays(1);
            case WEEKLY -> date.plusWeeks(1);
            case MONTHLY -> date.plusMonths(1);
        };
    }
}