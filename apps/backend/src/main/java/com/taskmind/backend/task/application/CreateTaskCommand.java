package com.taskmind.backend.task.application;

import com.taskmind.backend.task.domain.model.EnergyLevel;
import com.taskmind.backend.task.domain.model.TaskSource;
import com.taskmind.backend.task.domain.model.TaskStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateTaskCommand(
    UUID userId,
    UUID projectId,
    String title,
    String description,
    TaskStatus status,
    int priority,
    OffsetDateTime dueAt,
    Integer durationMinutes,
    EnergyLevel energyLevel,
    TaskSource source,
    BigDecimal confidence
) {
}
