package com.finora.backend.dto;

import com.finora.backend.domain.TransactionStatus;
import com.finora.backend.domain.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TransactionResponse {
    private String id;
    private String accountId;
    private BigDecimal amount;
    private String category;
    private String merchant;
    private TransactionType txnType;
    private TransactionStatus status;
    private Instant createdAt;
}