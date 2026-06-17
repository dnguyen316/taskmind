package com.taskmind.backend.integration.domain.model;

import java.time.Instant;
import java.util.UUID;

public record IntegrationImportRun(UUID id, Long version, UUID projectId, UUID projectLinkId, IntegrationProvider provider, String status, int importedCount, int skippedCount, String errorMessage, UUID requestedBy, Instant createdAt, Instant completedAt) {}
