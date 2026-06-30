package com.taskmind.backend.task.domain;

import com.taskmind.backend.task.domain.model.TaskLevel;
import com.taskmind.backend.task.domain.model.TaskType;
import com.taskmind.backend.tasktype.domain.model.TaskTypeDefinition;
import java.time.Instant;
import java.util.*;

public final class TaskTypeRules {
    private static final Map<String, TaskTypeDefinition> SYSTEM_TYPES = systemTypes();

    private TaskTypeRules() {}

    public static TaskTypeDefinition systemDefinition(String type) {
        if (type == null || type.isBlank()) throw new IllegalArgumentException("Task type is required");
        TaskTypeDefinition definition = SYSTEM_TYPES.get(type.trim().toUpperCase());
        if (definition == null) throw new IllegalArgumentException("Unknown task type");
        return definition;
    }

    public static void validate(TaskType type, TaskLevel level) {
        validate(type == null ? null : type.name(), level);
    }

    public static void validate(String type, TaskLevel level) {
        validate(systemDefinition(type), level);
    }

    public static void validate(TaskTypeDefinition definition, TaskLevel level) {
        if (definition == null || level == null) throw new IllegalArgumentException("Task type and level are required");
        if (!definition.allowedTaskLevels().contains(level)) {
            throw new IllegalArgumentException("Task type is not valid for its hierarchy level");
        }
    }

    private static Map<String, TaskTypeDefinition> systemTypes() {
        Instant now = Instant.EPOCH;
        Map<String, TaskTypeDefinition> types = new LinkedHashMap<>();
        add(types, TaskTypeDefinition.system(UUID.fromString("00000000-0000-0000-0000-000000000101"), "EPIC", "Epic", TaskLevel.EPIC, EnumSet.of(TaskLevel.EPIC), true, true, TaskTypeDefinition.SystemKind.EPIC, 10, now));
        add(types, TaskTypeDefinition.system(UUID.fromString("00000000-0000-0000-0000-000000000102"), "STORY", "Story", TaskLevel.STORY, EnumSet.of(TaskLevel.STORY), true, true, TaskTypeDefinition.SystemKind.STORY, 20, now));
        add(types, TaskTypeDefinition.system(UUID.fromString("00000000-0000-0000-0000-000000000103"), "TASK", "Task", TaskLevel.TASK, EnumSet.of(TaskLevel.TASK), false, false, TaskTypeDefinition.SystemKind.TASK, 30, now));
        add(types, TaskTypeDefinition.system(UUID.fromString("00000000-0000-0000-0000-000000000104"), "BUG", "Bug", TaskLevel.TASK, EnumSet.of(TaskLevel.TASK), false, false, TaskTypeDefinition.SystemKind.BUG, 40, now));
        add(types, TaskTypeDefinition.system(UUID.fromString("00000000-0000-0000-0000-000000000105"), "SUBTASK", "Subtask", TaskLevel.SUBTASK, EnumSet.of(TaskLevel.SUBTASK), false, false, TaskTypeDefinition.SystemKind.SUBTASK, 50, now));
        add(types, TaskTypeDefinition.system(UUID.fromString("00000000-0000-0000-0000-000000000106"), "MILESTONE", "Milestone", TaskLevel.TASK, EnumSet.of(TaskLevel.EPIC, TaskLevel.STORY, TaskLevel.TASK), true, true, TaskTypeDefinition.SystemKind.MILESTONE, 60, now));
        return Collections.unmodifiableMap(types);
    }

    private static void add(Map<String, TaskTypeDefinition> types, TaskTypeDefinition definition) {
        types.put(definition.key(), definition);
    }
}
