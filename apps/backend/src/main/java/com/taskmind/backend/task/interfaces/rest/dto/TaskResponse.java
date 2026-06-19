package com.taskmind.backend.task.interfaces.rest.dto;

import com.taskmind.backend.task.domain.model.EnergyLevel;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskLevel;
import com.taskmind.backend.task.domain.model.TaskSource;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.domain.model.TaskType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        Long version,
        UUID userId,
        UUID projectId,
        String taskKey,
        UUID assigneeId,
        UUID parentTaskId,
        TaskLevel taskLevel,
        TaskType taskType,
        Integer storyPoints,
        String releaseVersion,
        Instant deletedAt,
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
        Instant updatedAt) {
    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.id(),
                task.version(),
                task.userId(),
                task.projectId(),
                task.taskKey(),
                task.assigneeId(),
                task.parentTaskId(),
                task.taskLevel(),
                task.taskType(),
                task.storyPoints(),
                task.releaseVersion(),
                task.deletedAt(),
                task.title(),
                task.description(),
                task.status(),
                task.priority(),
                task.dueAt(),
                task.durationMinutes(),
                task.energyLevel(),
                task.source(),
                task.confidence(),
                task.createdAt(),
                task.updatedAt());
    }
}
