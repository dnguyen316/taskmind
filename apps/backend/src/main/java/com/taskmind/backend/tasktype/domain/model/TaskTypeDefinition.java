package com.taskmind.backend.tasktype.domain.model;

import java.time.Instant;
import java.util.UUID;

public record TaskTypeDefinition(
        UUID id,
        Long version,
        UUID projectId,
        String key,
        String name,
        String color,
        String icon,
        boolean system,
        boolean active,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {
    public TaskTypeDefinition {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Task type key is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Task type name is required");
        key = key.trim().toUpperCase();
        name = name.trim();
    }
}
