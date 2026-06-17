package com.taskmind.backend.integration.domain.model;

import java.time.Instant;
import java.util.UUID;

public record IntegrationConnection(
        UUID id,
        Long version,
        IntegrationProvider provider,
        String accountName,
        String baseUrl,
        String accountExternalId,
        UUID ownerUserId,
        String encryptedAccessToken,
        String encryptedRefreshToken,
        Instant tokenExpiresAt,
        String scopes,
        String status,
        Instant createdAt,
        Instant updatedAt) {}
