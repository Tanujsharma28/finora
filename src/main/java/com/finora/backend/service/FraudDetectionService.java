package com.finora.backend.service;

import com.finora.backend.domain.*;
import com.finora.backend.repository.FraudFlagRepository;
import com.finora.backend.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private static final BigDecimal SPIKE_MULTIPLIER = BigDecimal.valueOf(10);
    private static final BigDecimal MIN_BASELINE = BigDecimal.valueOf(500);

    private final TransactionRepository transactionRepository;
    private final FraudFlagRepository fraudFlagRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void evaluate(String transactionId, String accountId, BigDecimal amount) {
        Transaction txn = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transactionId));

        BigDecimal avgSpend = transactionRepository.findAverageAmountByAccountId(accountId);
        BigDecimal baseline = avgSpend.max(MIN_BASELINE);
        BigDecimal threshold = baseline.multiply(SPIKE_MULTIPLIER);

        TransactionStatus finalStatus;

        if (amount.compareTo(threshold) > 0) {
            int riskScore = calculateRiskScore(amount, threshold);

            FraudFlag flag = FraudFlag.builder()
                    .transaction(txn)
                    .reason(String.format("Amount %.2f exceeds %sx account baseline (%.2f)",
                            amount, SPIKE_MULTIPLIER, baseline))
                    .riskScore(riskScore)
                    .status(FraudFlagStatus.OPEN)
                    .build();

            fraudFlagRepository.save(flag);

            txn.setStatus(TransactionStatus.FLAGGED);
            transactionRepository.save(txn);
            finalStatus = TransactionStatus.FLAGGED;

            log.warn("[FraudDetection] FLAGGED txn={} amount={} riskScore={}", transactionId, amount, riskScore);
        } else {
            txn.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(txn);
            finalStatus = TransactionStatus.COMPLETED;

            log.info("[FraudDetection] Cleared txn={} amount={}", transactionId, amount);
        }

        messagingTemplate.convertAndSend(
                "/topic/transactions/" + accountId,
                Map.of(
                        "transactionId", transactionId,
                        "status", finalStatus.name(),
                        "accountId", accountId
                )
        );
    }

    private int calculateRiskScore(BigDecimal amount, BigDecimal threshold) {
        BigDecimal ratio = amount.divide(threshold, 2, java.math.RoundingMode.HALF_UP);
        int score = ratio.multiply(BigDecimal.valueOf(20)).intValue();
        return Math.min(score, 100);
    }
}