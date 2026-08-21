package com.example.telegram;

public record TelegramStatus(
    boolean enabled,
    boolean configured,
    boolean connected,
    String botUsername,
    String error
) {}
