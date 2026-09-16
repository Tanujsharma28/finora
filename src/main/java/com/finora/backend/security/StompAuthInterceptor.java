package com.finora.backend.security;

import com.finora.backend.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final AccountRepository accountRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            String token = null;
            if (authHeaders != null && !authHeaders.isEmpty() && authHeaders.get(0) != null
                    && authHeaders.get(0).startsWith("Bearer ")) {
                token = authHeaders.get(0).substring(7);
            }

            if (token == null || !jwtService.isValid(token)) {
                log.warn("[WS Auth] Rejected CONNECT — missing/invalid token");
                throw new IllegalArgumentException("Invalid or missing authentication token");
            }

            accessor.getSessionAttributes().put("userId", jwtService.extractUserId(token));
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            String userId = accessor.getSessionAttributes() != null
                    ? (String) accessor.getSessionAttributes().get("userId")
                    : null;

            if (userId == null) {
                throw new IllegalStateException("Unauthenticated subscription attempt");
            }

            if (destination != null &&
                    (destination.startsWith("/topic/notifications/") || destination.startsWith("/topic/transactions/"))) {
                String accountId = destination.substring(destination.lastIndexOf('/') + 1);
                if (!accountRepository.existsByIdAndUser_Id(accountId, userId)) {
                    log.warn("[WS Auth] userId={} tried to subscribe to accountId={} they don't own", userId, accountId);
                    throw new SecurityException("Not authorized to subscribe to this account");
                }
            }
        }

        return message;
    }
}