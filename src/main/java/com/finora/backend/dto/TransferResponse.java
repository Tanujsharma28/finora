package com.finora.backend.dto;

import com.finora.backend.domain.TransferStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransferResponse {
    private String id;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private TransferStatus status;
    private String failureReason;
    private Instant createdAt;
}