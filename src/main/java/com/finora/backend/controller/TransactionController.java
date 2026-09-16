package com.finora.backend.controller;

import com.finora.backend.dto.CreateTransactionRequest;
import com.finora.backend.dto.TransactionResponse;
import com.finora.backend.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody CreateTransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping
    public ResponseEntity<java.util.List<TransactionResponse>> getTransactions(@RequestParam String accountId) {
        return ResponseEntity.ok(transactionService.getTransactionsByAccount(accountId));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<TransactionResponse> reverseTransaction(@PathVariable String id) {
        return ResponseEntity.ok(transactionService.reverseTransaction(id));
    }
    @GetMapping("/{accountId}/export")
public ResponseEntity<String> exportTransactions(@PathVariable String accountId) {
    String csv = transactionService.exportTransactionsAsCsv(accountId);
    return ResponseEntity.ok()
            .header("Content-Disposition", "attachment; filename=\"statement-" + accountId + ".csv\"")
            .header("Content-Type", "text/csv")
            .body(csv);
}
}