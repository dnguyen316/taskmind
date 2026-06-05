package com.taskmind.backend.auth.interfaces.rest.dto;

public record AuthTokensResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresInSeconds
) {
}
