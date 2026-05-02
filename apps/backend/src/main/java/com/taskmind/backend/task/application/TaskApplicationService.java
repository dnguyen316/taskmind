package com.taskmind.backend.task.application;

import com.taskmind.backend.project.application.ProjectMembershipApplicationService;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional(readOnly = true)
public class TaskApplicationService {

    private final TaskRepository taskRepository;
    private final ProjectMembershipApplicationService projectMembershipApplicationService;

    public TaskApplicationService(
        TaskRepository taskRepository,
        ProjectMembershipApplicationService projectMembershipApplicationService
    ) {
        this.taskRepository = taskRepository;
        this.projectMembershipApplicationService = projectMembershipApplicationService;
    }

    @Transactional
    public Task create(CreateTaskCommand command) {
        projectMembershipApplicationService.validateMembership(command.projectId(), command.userId());

        var now = Instant.now();
        var task = new Task(
            UUID.randomUUID(),
            null,
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

    @Transactional(readOnly = true)
    public List<Task> list(Optional<UUID> userId, Optional<TaskStatus> status, boolean overdueOnly, int page, int size) {
        var now = OffsetDateTime.now();
        return taskRepository.findAll().stream()
            .filter(task -> userId.map(id -> id.equals(task.userId())).orElse(true))
            .filter(task -> status.map(taskStatus -> taskStatus == task.status()).orElse(true))
            .filter(task -> !overdueOnly || isOverdue(task, now))
            .sorted(Comparator.comparing(Task::createdAt).reversed())
            .skip((long) page * size)
            .limit(size)
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Task> findById(UUID id) {
        return taskRepository.findById(id);
    }

    @Transactional
    public Optional<Task> update(UUID id, UpdateTaskCommand command) {
        return taskRepository.findByIdForUpdate(id)
            .map(existing -> {
                var nextProjectId = command.projectId() != null ? command.projectId() : existing.projectId();
                projectMembershipApplicationService.validateMembership(nextProjectId, existing.userId());

                var updated = new Task(
                    existing.id(),
                    existing.version(),
                    existing.userId(),
                    nextProjectId,
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

    @Transactional
    public Optional<Task> updateStatus(UUID id, TaskStatus status) {
        return taskRepository.findByIdForUpdate(id)
            .map(existing -> taskRepository.save(existing.withStatus(status, Instant.now())));
    }

    @Transactional
    public Optional<Task> archive(UUID id) {
        return updateStatus(id, TaskStatus.ARCHIVED);
    }

    private boolean isOverdue(Task task, OffsetDateTime now) {
        return task.dueAt() != null
            && task.dueAt().isBefore(now)
            && task.status() != TaskStatus.DONE
            && task.status() != TaskStatus.ARCHIVED;
    }
}
