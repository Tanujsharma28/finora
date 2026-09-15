package com.finora.backend.dto;

import com.finora.backend.domain.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {

    @NotBlank(message = "userId is required")
    private String userId;

    @NotNull(message = "accountType is required")
    private AccountType accountType;
}