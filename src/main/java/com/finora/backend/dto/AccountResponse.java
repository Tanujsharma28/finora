package com.finora.backend.dto;

import com.finora.backend.domain.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class AccountResponse {
    private String id;
    private String accountNumber;
    private AccountType accountType;
    private BigDecimal balance;
    private Instant createdAt;
}
