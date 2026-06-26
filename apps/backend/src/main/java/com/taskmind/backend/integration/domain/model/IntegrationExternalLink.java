package com.taskmind.backend.integration.domain.model;

import java.time.Instant;
import java.util.UUID;

public record IntegrationExternalLink(
        UUID id,
        Long version,
        UUID taskId,
        UUID projectId,
        IntegrationProvider provider,
        String externalType,
        String externalId,
        String externalKey,
        String externalUrl,
        String direction,
        String metadataJson,
        String repositoryOwner,
        String repositoryName,
        Integer externalNumber,
        String gitSha,
        String checkRunId,
        Instant createdAt,
        Instant updatedAt) {}
