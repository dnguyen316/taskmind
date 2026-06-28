package com.taskmind.backend.task.domain;

import com.taskmind.backend.task.domain.model.TaskLevel;

public final class TaskTypeRules {
    private TaskTypeRules() {}
    public static void validate(com.taskmind.backend.task.domain.model.TaskType type, TaskLevel level) {
        validate(type == null ? null : type.name(), level);
    }
    public static void validate(String type, TaskLevel level) {
        if (type == null || level == null) throw new IllegalArgumentException("Task type and level are required");
        if (("EPIC".equals(type) && level != TaskLevel.EPIC)
            || ("STORY".equals(type) && level != TaskLevel.STORY)
            || ("SUBTASK".equals(type) && level != TaskLevel.SUBTASK)
            || ("MILESTONE".equals(type) && level == TaskLevel.SUBTASK)) {
            throw new IllegalArgumentException("Task type is not valid for its hierarchy level");
        }
    }
}
