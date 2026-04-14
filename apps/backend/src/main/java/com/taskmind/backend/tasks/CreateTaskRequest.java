package com.taskmind.backend.tasks;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateTaskRequest(
    @NotNull UUID userId,
    UUID projectId,
    @NotBlank String title,
    String description,
    @NotNull TaskStatus status,
    @Min(1) @Max(4) int priority,
    OffsetDateTime dueAt,
    @Positive Integer durationMinutes,
    EnergyLevel energyLevel,
    @NotNull TaskSource source,
    @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal confidence
) {
}
