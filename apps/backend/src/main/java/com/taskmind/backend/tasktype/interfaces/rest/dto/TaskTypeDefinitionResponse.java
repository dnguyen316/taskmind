package com.taskmind.backend.tasktype.interfaces.rest.dto;

import com.taskmind.backend.tasktype.domain.model.TaskTypeDefinition;
import java.time.Instant;
import java.util.UUID;

public record TaskTypeDefinitionResponse(
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
    public static TaskTypeDefinitionResponse from(TaskTypeDefinition d) {
        return new TaskTypeDefinitionResponse(d.id(), d.version(), d.projectId(), d.key(), d.name(), d.color(), d.icon(), d.system(), d.active(), d.sortOrder(), d.createdAt(), d.updatedAt());
    }
}
