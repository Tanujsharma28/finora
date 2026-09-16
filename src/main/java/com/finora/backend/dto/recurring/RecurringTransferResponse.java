package com.finora.backend.dto.recurring;

import com.finora.backend.domain.RecurringFrequency;
import com.finora.backend.domain.RecurringTransferStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record RecurringTransferResponse(
        String id,
        String fromAccountId,
        String toAccountId,
        BigDecimal amount,
        RecurringFrequency frequency,
        RecurringTransferStatus status,
        LocalDate nextRunDate,
        String description,
        Instant createdAt
) {
}