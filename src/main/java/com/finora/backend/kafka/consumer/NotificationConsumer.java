package com.finora.backend.kafka.consumer;

import com.finora.backend.dto.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(
            topics = "transaction-events",
            groupId = "notification-group",
            containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void consume(TransactionEvent event) {
        log.info("[Notification] New transaction initiated userId={} txnId={}", event.getUserId(), event.getTransactionId());

        messagingTemplate.convertAndSend(
                "/topic/notifications/" + event.getAccountId(),
                Map.of(
                        "type", "TRANSACTION_INITIATED",
                        "transactionId", event.getTransactionId(),
                        "accountId", event.getAccountId()
                )
        );
    }
}