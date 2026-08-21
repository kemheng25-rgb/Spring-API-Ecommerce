package com.example.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Fire-and-forget Telegram sender. A notification failure (bad token, Telegram outage, no
 * network) is logged and swallowed here - callers use this as a side channel and must not have
 * their own transaction/flow broken by it.
 *
 * Registered as a bean by {@link TelegramConfig} (not @Service) since this package sits outside
 * the host app's @ComponentScan root - see TelegramConfig's javadoc.
 */
@RequiredArgsConstructor
@Slf4j
public class TelegramNotifier {

    private final RestClient telegramRestClient;
    private final TelegramProperties properties;

    public void sendMessage(String text) {
        sendMessage(properties.chatId(), text);
    }

    public void sendMessage(String chatId, String text) {
        if (!properties.enabled()) {
            log.debug("[telegram] disabled, skipping message: {}", text);
            return;
        }
        if (properties.token() == null || properties.token().isBlank()
            || chatId == null || chatId.isBlank()) {
            log.warn("[telegram] enabled but token/chat-id missing, skipping message");
            return;
        }

        try {
            telegramRestClient.post()
                .uri("/bot{token}/sendMessage", properties.token())
                .body(Map.of("chat_id", chatId, "text", text))
                .retrieve()
                .toBodilessEntity();
        } catch (Exception ex) {
            log.error("[telegram] failed to send message to chat {}: {}", chatId, ex.getMessage());
        }
    }
}
