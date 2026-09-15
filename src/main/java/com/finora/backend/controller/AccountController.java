package com.finora.backend.controller;

import com.finora.backend.dto.AccountResponse;
import com.finora.backend.dto.CreateAccountRequest;
import com.finora.backend.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
public ResponseEntity<AccountResponse> getAccount(@PathVariable String id) {
    return ResponseEntity.ok(accountService.getAccountById(id));
}
@GetMapping
public ResponseEntity<java.util.List<AccountResponse>> getAccountsByUser(@RequestParam String userId) {
    return ResponseEntity.ok(accountService.getAccountsByUser(userId));
}
}