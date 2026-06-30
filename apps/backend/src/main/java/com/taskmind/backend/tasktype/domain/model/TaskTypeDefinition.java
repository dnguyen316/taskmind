package com.taskmind.backend.tasktype.domain.model;

import java.time.Instant;
import com.taskmind.backend.task.domain.model.TaskLevel;
import java.util.*;

public record TaskTypeDefinition(
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
        SystemKind systemKind,
        boolean system,
        boolean active,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt) {
    public enum SystemKind { EPIC, STORY, TASK, BUG, SUBTASK, MILESTONE }
    public TaskTypeDefinition {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Task type key is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Task type name is required");
        key = key.trim().toUpperCase();
        name = name.trim();
        defaultTaskLevel = defaultTaskLevel == null ? TaskLevel.TASK : defaultTaskLevel;
        allowedTaskLevels = allowedTaskLevels == null || allowedTaskLevels.isEmpty()
                ? Set.of(defaultTaskLevel)
                : Set.copyOf(allowedTaskLevels);
    }

    public static TaskTypeDefinition system(UUID id, String key, String name, TaskLevel defaultTaskLevel, Set<TaskLevel> allowedTaskLevels, boolean container, boolean allowChildren, SystemKind kind, Integer sortOrder, Instant now) {
        return new TaskTypeDefinition(id, null, null, key, name, null, null, defaultTaskLevel, allowedTaskLevels, container, allowChildren, kind, true, true, sortOrder, now, now);
    }
}
