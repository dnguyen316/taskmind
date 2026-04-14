package com.taskmind.backend.tasks;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateTaskRequest(
    UUID projectId,
    String title,
    String description,
    TaskStatus status,
    @Min(1) @Max(4) Integer priority,
    OffsetDateTime dueAt,
    @Positive Integer durationMinutes,
    EnergyLevel energyLevel
) {
}
