package com.taskmind.backend.aitaskresolution.interfaces.rest.dto;

import com.taskmind.backend.aitaskresolution.domain.*;
import java.time.Instant;
import java.util.UUID;

public record AiTaskResolutionJobResponse(
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
        Instant completedAt) {
    public static AiTaskResolutionJobResponse from(AiTaskResolutionJob job) {
        return new AiTaskResolutionJobResponse(
                job.id(),
                job.taskId(),
                job.projectId(),
                job.templateId(),
                job.githubProjectLinkId(),
                job.status(),
                job.requestedBy(),
                job.idempotencyKey(),
                job.novaRunId(),
                job.currentStep(),
                job.resultSummary(),
                job.errorCode(),
                job.createdAt(),
                job.updatedAt(),
                job.completedAt());
    }
}
