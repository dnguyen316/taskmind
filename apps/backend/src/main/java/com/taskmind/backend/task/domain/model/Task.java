package com.taskmind.backend.task.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Task(
    UUID id, @JsonIgnore Long version, UUID userId, UUID projectId, String taskKey, UUID assigneeId,
    UUID parentTaskId, TaskLevel taskLevel, String taskType, Integer storyPoints, String releaseVersion,
    Instant deletedAt, String title, String description, TaskStatus status, int priority, OffsetDateTime dueAt,
    Integer durationMinutes, EnergyLevel energyLevel, TaskSource source, BigDecimal confidence,
    Instant createdAt, Instant updatedAt
) {
    public Task {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Task title is required");
        TaskTypeRulesBridge.validate(taskType, taskLevel);
    }



    public Task(UUID id, Long version, UUID userId, UUID projectId, String taskKey, UUID assigneeId,
                UUID parentTaskId, TaskLevel taskLevel, TaskType taskType, Integer storyPoints, String releaseVersion,
                Instant deletedAt, String title, String description, TaskStatus status, int priority, OffsetDateTime dueAt,
                Integer durationMinutes, EnergyLevel energyLevel, TaskSource source, BigDecimal confidence,
                Instant createdAt, Instant updatedAt) {
        this(id, version, userId, projectId, taskKey, assigneeId, parentTaskId, taskLevel,
            taskType == null ? null : taskType.name(), storyPoints, releaseVersion, deletedAt, title, description,
            status, priority, dueAt, durationMinutes, energyLevel, source, confidence, createdAt, updatedAt);
    }

    public Task(UUID id, Long version, UUID userId, UUID projectId, String title, String description,
                TaskStatus status, int priority, OffsetDateTime dueAt, Integer durationMinutes,
                EnergyLevel energyLevel, TaskSource source, BigDecimal confidence, Instant createdAt, Instant updatedAt) {
        this(id, version, userId, projectId, null, null, null, TaskLevel.TASK, "TASK", null, null, null,
            title, description, status, priority, dueAt, durationMinutes, energyLevel, source, confidence, createdAt, updatedAt);
    }

    public Task withStatus(TaskStatus newStatus, Instant time) {
        return copy(parentTaskId, taskLevel, taskType, taskKey, assigneeId, storyPoints, releaseVersion,
            newStatus, deletedAt, time);
    }

    public Task withParent(UUID parentId, TaskLevel level, Instant time) {
        return copy(parentId, level, taskType, taskKey, assigneeId, storyPoints, releaseVersion, status, deletedAt, time);
    }

    public Task copy(UUID parentId, TaskLevel level, String type, String key, UUID assignee, Integer points,
                     String release, TaskStatus newStatus, Instant deleted, Instant time) {
        return new Task(id, version, userId, projectId, key, assignee, parentId, level, type, points, release, deleted,
            title, description, newStatus, priority, dueAt, durationMinutes, energyLevel, source, confidence, createdAt, time);
    }

    private static final class TaskTypeRulesBridge {
        private static void validate(String type, TaskLevel level) {
            com.taskmind.backend.task.domain.TaskTypeRules.validate(type, level);
        }
    }
}
