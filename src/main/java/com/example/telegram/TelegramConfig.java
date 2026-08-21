package com.example.telegram;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Registered via META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 * so this package wires itself in on any Spring Boot classpath, regardless of the host app's
 * base package / @ComponentScan root - that's what makes it copy-paste portable.
 */
@AutoConfiguration
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramConfig {

    @Bean
    RestClient telegramRestClient(TelegramProperties properties) {
        return RestClient.builder()
            .baseUrl(properties.apiBaseUrl())
            .build();
    }

    @Bean
    TelegramNotifier telegramNotifier(RestClient telegramRestClient, TelegramProperties properties) {
        return new TelegramNotifier(telegramRestClient, properties);
    }

    @Bean
    TelegramController telegramController(TelegramNotifier telegramNotifier) {
        return new TelegramController(telegramNotifier);
    }
}
