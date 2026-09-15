package com.finora.backend.service;

import com.finora.backend.domain.Transaction;
import com.finora.backend.domain.TransactionStatus;
import com.finora.backend.repository.FraudFlagRepository;
import com.finora.backend.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FraudFlagRepository fraudFlagRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    private Transaction txn;

    @BeforeEach
    void setUp() {
        txn = Transaction.builder()
                .id("txn-1")
                .status(TransactionStatus.PENDING)
                .build();
    }

    @Test
    void shouldClearTransactionWithinNormalRange() {
        when(transactionRepository.findById("txn-1")).thenReturn(Optional.of(txn));
        when(transactionRepository.findAverageAmountByAccountId("acc-1"))
                .thenReturn(BigDecimal.valueOf(500));

        fraudDetectionService.evaluate("txn-1", "acc-1", BigDecimal.valueOf(300));

        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(fraudFlagRepository, never()).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/transactions/acc-1"), any(Object.class));
    }

    @Test
    void shouldFlagTransactionExceedingSpikeThreshold() {
        when(transactionRepository.findById("txn-1")).thenReturn(Optional.of(txn));
        when(transactionRepository.findAverageAmountByAccountId("acc-1"))
                .thenReturn(BigDecimal.valueOf(500));

        fraudDetectionService.evaluate("txn-1", "acc-1", BigDecimal.valueOf(95000));

        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.FLAGGED);
        verify(fraudFlagRepository, times(1)).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/transactions/acc-1"), any(Object.class));
    }

    @Test
    void shouldUseMinimumBaselineWhenAverageIsLow() {
        when(transactionRepository.findById("txn-1")).thenReturn(Optional.of(txn));
        when(transactionRepository.findAverageAmountByAccountId("acc-1"))
                .thenReturn(BigDecimal.ZERO);

        // baseline floor is ₹500, so 10x = ₹5000 threshold
        fraudDetectionService.evaluate("txn-1", "acc-1", BigDecimal.valueOf(4000));

        assertThat(txn.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
    }
}