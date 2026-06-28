package com.taskmind.backend.aitaskresolution.domain;

import java.time.Instant;
import java.util.UUID;

public record AiTaskResolutionJob(
        UUID id,
        UUID taskId,
        UUID projectId,
        UUID templateId,
        UUID githubProjectLinkId,
        AiTaskResolutionJobStatus status,
        UUID requestedBy,
        String idempotencyKey,
        UUID novaRunId,
        String currentStep,
        String resultSummary,
        String errorCode,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt) {}
