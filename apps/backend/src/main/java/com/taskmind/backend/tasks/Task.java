package com.taskmind.backend.tasks;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Task(
    UUID id,
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
    BigDecimal confidence,
    Instant createdAt,
    Instant updatedAt
) {
}
