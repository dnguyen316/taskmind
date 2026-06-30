package com.taskmind.backend.tasktype.application;

import com.taskmind.backend.task.domain.model.TaskLevel;
import com.taskmind.backend.tasktype.domain.model.TaskTypeDefinition;
import com.taskmind.backend.tasktype.domain.repository.TaskTypeRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskTypeApplicationService {
    private final TaskTypeRepository taskTypes;

    public TaskTypeApplicationService(TaskTypeRepository taskTypes) {
        this.taskTypes = taskTypes;
    }

    public List<TaskTypeDefinition> listActive(UUID projectId) {
        return taskTypes.findActive(projectId);
    }

    public TaskTypeDefinition requireActiveByKey(UUID projectId, String key) {
        return taskTypes.findActiveByKey(projectId, normalize(key))
                .orElseThrow(() -> new IllegalArgumentException("Unknown task type"));
    }

    @Transactional
    public TaskTypeDefinition create(UUID projectId, String key, String name, String color, String icon, Integer sortOrder) {
        Instant now = Instant.now();
        return taskTypes.save(new TaskTypeDefinition(UUID.randomUUID(), null, projectId, normalize(key), name, color, icon, TaskLevel.TASK, Set.of(TaskLevel.TASK), false, false, null, false, true, sortOrder, now, now));
    }

    @Transactional
    public Optional<TaskTypeDefinition> update(UUID id, String name, String color, String icon, Boolean active, Integer sortOrder) {
        return taskTypes.findById(id).map(existing -> taskTypes.save(new TaskTypeDefinition(
                existing.id(), existing.version(), existing.projectId(), existing.key(),
                name != null ? name : existing.name(),
                color != null ? color : existing.color(),
                icon != null ? icon : existing.icon(),
                existing.defaultTaskLevel(), existing.allowedTaskLevels(), existing.container(), existing.allowChildren(), existing.systemKind(),
                existing.system(), active != null ? active : existing.active(),
                sortOrder != null ? sortOrder : existing.sortOrder(), existing.createdAt(), Instant.now())));
    }

    private String normalize(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Task type key is required");
        return key.trim().toUpperCase();
    }
}
