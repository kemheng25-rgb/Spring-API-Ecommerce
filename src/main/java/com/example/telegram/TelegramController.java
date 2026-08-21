package com.example.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only status + a manual test-send, for an admin UI to show "Telegram: connected".
 * Not @RestController-scanned by the host app (see TelegramConfig) - registered as a bean
 * there, which is enough for Spring MVC to still pick up its @GetMapping/@PostMapping methods.
 */
@RestController
@RequiredArgsConstructor
public class TelegramController {

    private final TelegramNotifier telegramNotifier;

    @GetMapping("/api/v1/telegram/status")
    public ResponseEntity<TelegramStatus> status() {
        return ResponseEntity.ok(telegramNotifier.fetchStatus());
    }

    @PostMapping("/api/v1/telegram/test")
    public ResponseEntity<TelegramTestResult> sendTest() {
        boolean sent = telegramNotifier.sendMessage("Test message from java-api admin settings.");
        return ResponseEntity.ok(new TelegramTestResult(sent));
    }

    public record TelegramTestResult(boolean sent) {}
}
