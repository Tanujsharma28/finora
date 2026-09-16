package com.finora.backend.dto.recurring;

import com.finora.backend.domain.RecurringFrequency;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateRecurringTransferRequest(

        @NotBlank(message = "Source account ID is required")
        String fromAccountId,

        @NotBlank(message = "Destination account ID is required")
        String toAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        @Digits(integer = 13, fraction = 2, message = "Amount must have at most 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "Frequency is required")
        RecurringFrequency frequency,

        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date cannot be in the past")
        LocalDate startDate,

        String description
) {
}