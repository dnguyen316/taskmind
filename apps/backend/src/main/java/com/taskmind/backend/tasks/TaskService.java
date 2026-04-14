package com.taskmind.backend.tasks;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    private final ConcurrentMap<UUID, Task> tasksById = new ConcurrentHashMap<>();

    public Task create(CreateTaskRequest request) {
        var now = Instant.now();
        var id = UUID.randomUUID();

        var task = new Task(
            id,
            request.userId(),
            request.projectId(),
            request.title().trim(),
            request.description(),
            request.status(),
            request.priority(),
            request.dueAt(),
            request.durationMinutes(),
            request.energyLevel(),
            request.source(),
            request.confidence(),
            now,
            now
        );

        tasksById.put(id, task);
        return task;
    }

    public List<Task> list(Optional<UUID> userId) {
        var tasks = new ArrayList<>(tasksById.values());
        return tasks.stream()
            .filter(task -> userId.map(id -> id.equals(task.userId())).orElse(true))
            .sorted(Comparator.comparing(Task::createdAt).reversed())
            .toList();
    }

    public Optional<Task> update(UUID taskId, UpdateTaskRequest request) {
        return Optional.ofNullable(tasksById.computeIfPresent(taskId, (ignored, existing) -> {
            var now = Instant.now();
            return new Task(
                existing.id(),
                existing.userId(),
                request.projectId() != null ? request.projectId() : existing.projectId(),
                request.title() != null ? request.title().trim() : existing.title(),
                request.description() != null ? request.description() : existing.description(),
                request.status() != null ? request.status() : existing.status(),
                request.priority() != null ? request.priority() : existing.priority(),
                request.dueAt() != null ? request.dueAt() : existing.dueAt(),
                request.durationMinutes() != null ? request.durationMinutes() : existing.durationMinutes(),
                request.energyLevel() != null ? request.energyLevel() : existing.energyLevel(),
                existing.source(),
                existing.confidence(),
                existing.createdAt(),
                now
            );
        }));
    }
}
