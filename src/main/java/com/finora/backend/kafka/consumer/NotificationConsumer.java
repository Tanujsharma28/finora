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

        String message = String.format(
                "%s of ₹%.2f %s",
                event.getTxnType().name(),
                event.getAmount(),
                event.getMerchant() != null ? "at " + event.getMerchant() : "initiated"
        );

        messagingTemplate.convertAndSend(
                "/topic/notifications/" + event.getAccountId(),
                Map.of(
                        "type", "TRANSACTION_INITIATED",
                        "message", message,
                        "transactionId", event.getTransactionId(),
                        "accountId", event.getAccountId()
                )
        );
    }
}