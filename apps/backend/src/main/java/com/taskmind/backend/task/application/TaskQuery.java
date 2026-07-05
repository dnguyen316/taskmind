package com.taskmind.backend.task.application;

import com.taskmind.backend.task.domain.model.TaskStatus;
import java.util.UUID;

public record TaskQuery(
        UUID userId,
        TaskStatus status,
        Boolean dueToday,
        Boolean overdue,
        Boolean blocked,
        Boolean unassigned,
        Boolean noDueDate,
        Boolean stale,
        Boolean archived,
        Integer priority,
        UUID projectId,
        UUID assigneeId,
        String sort,
        int page,
        int size) {}
