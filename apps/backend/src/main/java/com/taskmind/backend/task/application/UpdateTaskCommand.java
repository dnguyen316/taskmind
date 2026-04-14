package com.taskmind.backend.task.application;

import com.taskmind.backend.task.domain.model.EnergyLevel;
import com.taskmind.backend.task.domain.model.TaskStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateTaskCommand(
    UUID projectId,
    String title,
    String description,
    TaskStatus status,
    Integer priority,
    OffsetDateTime dueAt,
    Integer durationMinutes,
    EnergyLevel energyLevel
) {
}
