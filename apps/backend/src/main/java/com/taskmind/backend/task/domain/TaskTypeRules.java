package com.taskmind.backend.task.domain;

import com.taskmind.backend.task.domain.model.TaskLevel;
import com.taskmind.backend.task.domain.model.TaskType;

public final class TaskTypeRules {
    private TaskTypeRules() {}
    public static void validate(TaskType type, TaskLevel level) {
        if (type == null || level == null) throw new IllegalArgumentException("Task type and level are required");
        if ((type == TaskType.EPIC && level != TaskLevel.EPIC)
            || (type == TaskType.STORY && level != TaskLevel.STORY)
            || (type == TaskType.SUBTASK && level != TaskLevel.SUBTASK)
            || (type == TaskType.MILESTONE && level == TaskLevel.SUBTASK)) {
            throw new IllegalArgumentException("Task type is not valid for its hierarchy level");
        }
    }
}
