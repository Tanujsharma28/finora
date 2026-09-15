package com.finora.backend.dto;

import com.finora.backend.domain.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private String transactionId;
    private String accountId;
    private String userId;
    private BigDecimal amount;
    private String category;
    private String merchant;
    private TransactionType txnType;
    private Instant createdAt;
}