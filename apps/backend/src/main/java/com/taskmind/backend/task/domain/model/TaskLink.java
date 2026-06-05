package com.taskmind.backend.task.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TaskLink(UUID id, Long version, UUID sourceTaskId, UUID targetTaskId, TaskLinkType linkType,
                       UUID createdByUserId, Instant createdAt) {
    public TaskLink {
        if (sourceTaskId == null || targetTaskId == null || sourceTaskId.equals(targetTaskId)) {
            throw new IllegalArgumentException("A task link must connect two different tasks");
        }
        if (linkType == null) throw new IllegalArgumentException("Link type is required");
    }
}
