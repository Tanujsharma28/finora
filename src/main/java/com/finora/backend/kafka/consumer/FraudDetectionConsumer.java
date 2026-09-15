package com.finora.backend.kafka.consumer;

import com.finora.backend.dto.TransactionEvent;
import com.finora.backend.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudDetectionConsumer {

    private final FraudDetectionService fraudDetectionService;

    @KafkaListener(
            topics = "transaction-events",
            groupId = "fraud-detection-group",
            containerFactory = "fraudKafkaListenerContainerFactory"
    )
    public void consume(TransactionEvent event) {
        log.info("[FraudDetection] Evaluating txn={} amount={}", event.getTransactionId(), event.getAmount());
        fraudDetectionService.evaluate(event.getTransactionId(), event.getAccountId(), event.getAmount());
    }
}