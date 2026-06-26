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
        String repositoryOwner,
        String repositoryName,
        String defaultBranch,
        String installationId,
        String accountId,
        String allowedOperationsJson,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt) {}
