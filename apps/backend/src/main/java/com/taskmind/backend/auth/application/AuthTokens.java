package com.taskmind.backend.auth.application;

public record AuthTokens(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInSeconds
) {
}
