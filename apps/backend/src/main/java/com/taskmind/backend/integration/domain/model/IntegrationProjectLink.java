package com.taskmind.backend.integration.domain.model;

import java.time.Instant;
import java.util.UUID;

public record IntegrationProjectLink(
        UUID id,
        Long version,
        UUID projectId,
        UUID connectionId,
        IntegrationProvider provider,
        String externalProjectId,
        String externalProjectKey,
        String externalProjectName,
        String metadataJson,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt) {}
