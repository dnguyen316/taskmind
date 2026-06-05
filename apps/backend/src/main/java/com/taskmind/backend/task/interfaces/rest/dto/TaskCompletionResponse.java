package com.taskmind.backend.task.interfaces.rest.dto;

import com.taskmind.backend.task.domain.model.TaskStatus;
import java.time.Instant;
import java.util.UUID;

public record TaskCompletionResponse(
    UUID id,
    TaskStatus status,
    boolean completed,
    Instant updatedAt
) {
}
