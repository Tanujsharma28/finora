package com.finora.backend.service;

import com.finora.backend.domain.RecurringTransfer;
import com.finora.backend.domain.RecurringTransferStatus;
import com.finora.backend.repository.RecurringTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransferScheduler {

    private final RecurringTransferRepository recurringTransferRepository;
    private final RecurringTransferItemProcessor itemProcessor;

    // Runs once daily at 2 AM
    @Scheduled(cron = "0 0 2 * * *")
    public void runDueTransfers() {
        LocalDate today = LocalDate.now();

        List<RecurringTransfer> due = recurringTransferRepository
                .findByNextRunDateLessThanEqualAndStatus(today, RecurringTransferStatus.ACTIVE);

        log.info("[RecurringScheduler] Found {} due recurring transfers for {}", due.size(), today);

        for (RecurringTransfer recurringTransfer : due) {
            itemProcessor.process(recurringTransfer.getId(), today);
        }
    }
}