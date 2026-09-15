package com.finora.backend.service;

import com.finora.backend.domain.*;
import com.finora.backend.dto.TransferRequest;
import com.finora.backend.dto.TransferResponse;
import com.finora.backend.repository.AccountRepository;
import com.finora.backend.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferLegService transferLegService;

    @InjectMocks
    private TransferService transferService;

    private Account fromAccount;
    private Account toAccount;

    @BeforeEach
    void setUp() {
        fromAccount = Account.builder()
                .id("acc-from")
                .balance(BigDecimal.valueOf(5000))
                .build();
        toAccount = Account.builder()
                .id("acc-to")
                .balance(BigDecimal.ZERO)
                .build();
    }

    @Test
    void shouldCompleteTransferWhenBothLegsSucceed() {
        TransferRequest request = buildRequest("key-1");

        when(transferRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-from")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-to")).thenReturn(Optional.of(toAccount));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction debitTxn = Transaction.builder().id("debit-1").build();
        Transaction creditTxn = Transaction.builder().id("credit-1").build();
        when(transferLegService.debitLeg(eq("acc-from"), any())).thenReturn(debitTxn);
        when(transferLegService.creditLeg(eq("acc-to"), any())).thenReturn(creditTxn);

        TransferResponse response = transferService.transfer(request);

        assertThat(response.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        verify(transferLegService, never()).compensate(any(), any());
    }

    @Test
    void shouldCompensateWhenCreditLegFails() {
        TransferRequest request = buildRequest("key-2");

        when(transferRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-from")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-to")).thenReturn(Optional.of(toAccount));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));

        Transaction debitTxn = Transaction.builder().id("debit-1").build();
        when(transferLegService.debitLeg(eq("acc-from"), any())).thenReturn(debitTxn);
        when(transferLegService.creditLeg(eq("acc-to"), any()))
                .thenThrow(new IllegalStateException("Simulated credit failure"));

        TransferResponse response = transferService.transfer(request);

        assertThat(response.getStatus()).isEqualTo(TransferStatus.COMPENSATED);
        assertThat(response.getFailureReason()).contains("Simulated credit failure");
        verify(transferLegService, times(1)).compensate(eq("acc-from"), any());
    }

    @Test
    void shouldFailWhenDebitLegFails() {
        TransferRequest request = buildRequest("key-3");

        when(transferRepository.findByIdempotencyKey("key-3")).thenReturn(Optional.empty());
        when(accountRepository.findById("acc-from")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findById("acc-to")).thenReturn(Optional.of(toAccount));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(inv -> inv.getArgument(0));

        when(transferLegService.debitLeg(eq("acc-from"), any()))
                .thenThrow(new IllegalStateException("Simulated debit failure"));

        TransferResponse response = transferService.transfer(request);

        assertThat(response.getStatus()).isEqualTo(TransferStatus.FAILED);
        verify(transferLegService, never()).creditLeg(any(), any());
        verify(transferLegService, never()).compensate(any(), any());
    }

    @Test
    void shouldReturnExistingTransferForDuplicateIdempotencyKey() {
        Transfer existing = Transfer.builder()
                .id("existing-id")
                .idempotencyKey("dup-key")
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(BigDecimal.valueOf(500))
                .status(TransferStatus.COMPLETED)
                .build();

        when(transferRepository.findByIdempotencyKey("dup-key")).thenReturn(Optional.of(existing));

        TransferRequest request = buildRequest("dup-key");
        TransferResponse response = transferService.transfer(request);

        assertThat(response.getId()).isEqualTo("existing-id");
        verify(accountRepository, never()).findById(anyString());
        verify(transferLegService, never()).debitLeg(any(), any());
    }

    private TransferRequest buildRequest(String idempotencyKey) {
        TransferRequest request = new TransferRequest();
        request.setFromAccountId("acc-from");
        request.setToAccountId("acc-to");
        request.setAmount(BigDecimal.valueOf(1000));
        request.setIdempotencyKey(idempotencyKey);
        return request;
    }
}