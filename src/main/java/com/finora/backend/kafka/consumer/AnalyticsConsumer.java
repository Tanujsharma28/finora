package com.finora.backend.kafka.consumer;

import com.finora.backend.domain.TransactionType;
import com.finora.backend.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsConsumer {

    private final StringRedisTemplate redisTemplate;

    @KafkaListener(
            topics = "transaction-events",
            groupId = "analytics-group",
            containerFactory = "analyticsKafkaListenerContainerFactory"
    )
    public void consume(TransactionEvent event) {
        if (event.getTxnType() != TransactionType.DEBIT) {
            log.info("[Analytics] Skipping non-debit txn={} for account={}", event.getTransactionId(), event.getAccountId());
            return;
        }

        String key = "analytics:" + event.getAccountId();
        String category = event.getCategory() != null ? event.getCategory() : "OTHER";

        redisTemplate.opsForHash().increment(key, category, event.getAmount().doubleValue());

        log.info("[Analytics] Recorded category={} amount={} for account={}",
                category, event.getAmount(), event.getAccountId());
    }
}