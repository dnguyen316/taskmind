package com.taskmind.backend.specbreakdown.application;

import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownJobStatus;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownJobType;
import java.time.Instant;
import java.util.UUID;

public record SpecBreakdownProcessingJob(
        UUID id,
        Long version,
        UUID draftId,
        UUID userId,
        SpecBreakdownJobType aiJobType,
        SpecBreakdownJobStatus status,
        String checkpoint,
        UUID novaRunId,
        String errorMessage,
        boolean requestedCancel,
        boolean paused,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt) {}
