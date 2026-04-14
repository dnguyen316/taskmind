package com.taskmind.backend.task.interfaces.rest.dto;

import com.taskmind.backend.task.domain.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(
    @NotNull TaskStatus status
) {
}
