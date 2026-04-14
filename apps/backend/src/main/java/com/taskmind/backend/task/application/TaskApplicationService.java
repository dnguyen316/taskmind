package com.taskmind.backend.task.application;

import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TaskApplicationService {

    private final TaskRepository taskRepository;

    public TaskApplicationService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public Task create(CreateTaskCommand command) {
        var now = Instant.now();
        var task = new Task(
            UUID.randomUUID(),
            command.userId(),
            command.projectId(),
            command.title().trim(),
            command.description(),
            command.status(),
            command.priority(),
            command.dueAt(),
            command.durationMinutes(),
            command.energyLevel(),
            command.source(),
            command.confidence(),
            now,
            now
        );
        return taskRepository.save(task);
    }

    public List<Task> list(Optional<UUID> userId) {
        return taskRepository.findAll().stream()
            .filter(task -> userId.map(id -> id.equals(task.userId())).orElse(true))
            .sorted(Comparator.comparing(Task::createdAt).reversed())
            .toList();
    }

    public Optional<Task> findById(UUID id) {
        return taskRepository.findById(id);
    }

    public Optional<Task> update(UUID id, UpdateTaskCommand command) {
        return taskRepository.findById(id)
            .map(existing -> {
                var updated = new Task(
                    existing.id(),
                    existing.userId(),
                    command.projectId() != null ? command.projectId() : existing.projectId(),
                    command.title() != null ? command.title().trim() : existing.title(),
                    command.description() != null ? command.description() : existing.description(),
                    command.status() != null ? command.status() : existing.status(),
                    command.priority() != null ? command.priority() : existing.priority(),
                    command.dueAt() != null ? command.dueAt() : existing.dueAt(),
                    command.durationMinutes() != null ? command.durationMinutes() : existing.durationMinutes(),
                    command.energyLevel() != null ? command.energyLevel() : existing.energyLevel(),
                    existing.source(),
                    existing.confidence(),
                    existing.createdAt(),
                    Instant.now()
                );
                return taskRepository.save(updated);
            });
    }

    public Optional<Task> updateStatus(UUID id, TaskStatus status) {
        return taskRepository.findById(id)
            .map(existing -> taskRepository.save(existing.withStatus(status, Instant.now())));
    }
}
