package com.taskmind.ai.contracts.audit;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.taskmind.ai.contracts.AiCapabilityId;
import com.taskmind.ai.contracts.AiProviderId;
import com.taskmind.ai.contracts.AiRunStatus;
import java.time.Instant;
import java.util.UUID;

/** Provider-neutral audit summary for Core troubleshooting facades. */
@JsonPropertyOrder({
    "runId",
    "status",
    "providerId",
    "capabilityId",
    "modelId",
    "correlationId",
    "createdAt",
    "startedAt",
    "completedAt"
})
public record AiRunSummary(
        UUID runId,
        AiRunStatus status,
        AiProviderId providerId,
        AiCapabilityId capabilityId,
        String modelId,
        String correlationId,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt) {}
