package com.taskmind.backend.aitaskresolution.interfaces.rest.dto;

import com.taskmind.backend.aitaskresolution.domain.*;
import java.time.Instant;
import java.util.UUID;

public record AiTaskResolutionJobResponse(UUID id, UUID taskId, UUID projectId, UUID templateId, UUID githubProjectLinkId, AiTaskResolutionJobStatus status, UUID requestedBy, String idempotencyKey, UUID novaRunId, String currentStep, String resultSummary, String errorCode, Instant createdAt, Instant updatedAt, Instant completedAt) {
    public static AiTaskResolutionJobResponse from(AiTaskResolutionJob j) { return new AiTaskResolutionJobResponse(j.id(), j.taskId(), j.projectId(), j.templateId(), j.githubProjectLinkId(), j.status(), j.requestedBy(), j.idempotencyKey(), j.novaRunId(), j.currentStep(), j.resultSummary(), j.errorCode(), j.createdAt(), j.updatedAt(), j.completedAt()); }
}
