package com.taskmind.backend.auth.interfaces.rest.dto;

public record AuthTokensResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds
) {
}
