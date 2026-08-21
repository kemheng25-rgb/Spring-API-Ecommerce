package com.example.telegram;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    public boolean sendMessage(String text) {
        return sendMessage(properties.chatId(), text);
    }

    public boolean sendMessage(String chatId, String text) {
        if (!properties.enabled()) {
            log.debug("[telegram] disabled, skipping message: {}", text);
            return false;
        }
        if (!isConfigured() || chatId == null || chatId.isBlank()) {
            log.warn("[telegram] enabled but token/chat-id missing, skipping message");
            return false;
        }

        try {
            telegramRestClient.post()
                .uri("/bot{token}/sendMessage", properties.token())
                .body(Map.of("chat_id", chatId, "text", text, "parse_mode", "HTML"))
                .retrieve()
                .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.error("[telegram] failed to send message to chat {}: {}", chatId, ex.getMessage());
            return false;
        }
    }

    /**
     * Messages are sent with parse_mode=HTML (see sendMessage), so any dynamic text interpolated
     * into a message - a product name, a buyer's name, a free-text reason - must run through this
     * first or a stray &amp;/&lt;/&gt; breaks Telegram's parser and the whole message is rejected.
     */
    public static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Calls Telegram's getMe to prove the token is valid and the API is reachable. */
    public TelegramStatus fetchStatus() {
        if (!properties.enabled()) {
            return new TelegramStatus(false, isConfigured(), false, null, "disabled");
        }
        if (!isConfigured()) {
            return new TelegramStatus(true, false, false, null, "token or chat-id not set");
        }

        try {
            TelegramMeResponse response = telegramRestClient.get()
                .uri("/bot{token}/getMe", properties.token())
                .retrieve()
                .body(TelegramMeResponse.class);

            if (response != null && response.ok() && response.result() != null) {
                return new TelegramStatus(true, true, true, response.result().username(), null);
            }
            return new TelegramStatus(true, true, false, null, "unexpected response from Telegram");
        } catch (Exception ex) {
            log.warn("[telegram] status check failed: {}", ex.getMessage());
            return new TelegramStatus(true, true, false, null, ex.getMessage());
        }
    }

    private boolean isConfigured() {
        return properties.token() != null && !properties.token().isBlank()
            && properties.chatId() != null && !properties.chatId().isBlank();
    }

    private record TelegramMeResponse(boolean ok, TelegramMe result) {}

    private record TelegramMe(String username, @JsonProperty("first_name") String firstName) {}
}
