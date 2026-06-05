package com.taskmind.backend.task.domain;

import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskLevel;
import java.util.List;

public final class TaskHierarchyRules {
    public static final int MAX_DEPTH = 4;
    private TaskHierarchyRules() {}

    public static void validateParent(Task child, Task parent, List<Task> ancestors) {
        if (child.id().equals(parent.id())) throw new IllegalArgumentException("A task cannot be its own parent");
        if (child.projectId() == null || !child.projectId().equals(parent.projectId())) {
            throw new IllegalArgumentException("Parent and child must belong to the same project");
        }
        if (ancestors.stream().anyMatch(task -> task.id().equals(child.id()))) {
            throw new IllegalArgumentException("Task hierarchy cannot contain a cycle");
        }
        if (ancestors.size() + 2 > MAX_DEPTH) throw new IllegalArgumentException("Task hierarchy exceeds maximum depth");
        if (!isDirectChild(parent.taskLevel(), child.taskLevel())) throw new IllegalArgumentException("Invalid task hierarchy level");
    }

    public static boolean isDirectChild(TaskLevel parent, TaskLevel child) {
        return (parent == TaskLevel.EPIC && child == TaskLevel.STORY)
            || (parent == TaskLevel.STORY && child == TaskLevel.TASK)
            || (parent == TaskLevel.TASK && child == TaskLevel.SUBTASK);
    }
}
