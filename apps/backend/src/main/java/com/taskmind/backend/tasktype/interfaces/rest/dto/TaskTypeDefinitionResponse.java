package com.taskmind.backend.tasktype.interfaces.rest.dto;

import com.taskmind.backend.task.domain.model.TaskLevel;
import com.taskmind.backend.tasktype.domain.model.TaskTypeDefinition;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TaskTypeDefinitionResponse(
        UUID id,
        Long version,
        UUID projectId,
        String key,
        String name,
        String color,
        String icon,
        TaskLevel defaultTaskLevel,
        Set<TaskLevel> allowedTaskLevels,
        boolean container,
        boolean allowChildren,
        TaskTypeDefinition.SystemKind systemKind,
        boolean system,
        boolean active,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {
    public static TaskTypeDefinitionResponse from(TaskTypeDefinition definition) {
        return new TaskTypeDefinitionResponse(definition.id(), definition.version(), definition.projectId(), definition.key(), definition.name(), definition.color(), definition.icon(), definition.defaultTaskLevel(), definition.allowedTaskLevels(), definition.container(), definition.allowChildren(), definition.systemKind(), definition.system(), definition.active(), definition.sortOrder(), definition.createdAt(), definition.updatedAt());
    }
}
