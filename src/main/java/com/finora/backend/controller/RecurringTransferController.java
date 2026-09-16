package com.finora.backend.controller;

import com.finora.backend.dto.recurring.CreateRecurringTransferRequest;
import com.finora.backend.dto.recurring.RecurringTransferResponse;
import com.finora.backend.service.RecurringTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transfers")
@RequiredArgsConstructor
public class RecurringTransferController {

    private final RecurringTransferService recurringTransferService;

    @PostMapping
    public ResponseEntity<RecurringTransferResponse> create(
            @Valid @RequestBody CreateRecurringTransferRequest request) {
        RecurringTransferResponse response = recurringTransferService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RecurringTransferResponse>> getByAccount(
            @RequestParam String accountId) {
        return ResponseEntity.ok(recurringTransferService.getByAccount(accountId));
    }

    @PatchMapping("/{id}/pause")
    public ResponseEntity<RecurringTransferResponse> pause(@PathVariable String id) {
        return ResponseEntity.ok(recurringTransferService.pause(id));
    }

    @PatchMapping("/{id}/resume")
    public ResponseEntity<RecurringTransferResponse> resume(@PathVariable String id) {
        return ResponseEntity.ok(recurringTransferService.resume(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RecurringTransferResponse> cancel(@PathVariable String id) {
        return ResponseEntity.ok(recurringTransferService.cancel(id));
    }
}