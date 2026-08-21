package com.example.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Bind target for {@code telegram.bot.*}. This whole {@code com.example.telegram} package has
 * no dependency on the rest of the app - copy the package as-is into another Spring Boot
 * project (any base package) and it works unchanged.
 */
@ConfigurationProperties(prefix = "telegram.bot")
public record TelegramProperties(
    boolean enabled,
    String token,
    String chatId,
    String apiBaseUrl
) {
    public TelegramProperties {
        if (apiBaseUrl == null || apiBaseUrl.isBlank()) {
            apiBaseUrl = "https://api.telegram.org";
        }
    }
}
