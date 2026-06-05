package com.taskmind.backend.task.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.project.application.ProjectMembershipApplicationService;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskApplicationService {

    private final TaskRepository taskRepository;
    private final ProjectMembershipApplicationService projectMembershipApplicationService;

    public TaskApplicationService(
            TaskRepository taskRepository,
            ProjectMembershipApplicationService projectMembershipApplicationService) {
        this.taskRepository = taskRepository;
        this.projectMembershipApplicationService = projectMembershipApplicationService;
    }

    @Transactional
    public Task create(AuthenticatedUser requester, CreateTaskCommand command) {
        var effectiveUserId = requester.isPrivileged() ? command.userId() : requester.userId();
        projectMembershipApplicationService.validateMembership(
                command.projectId(), effectiveUserId);

        var now = Instant.now();
        var task =
                new Task(
                        UUID.randomUUID(),
                        null,
                        effectiveUserId,
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
                        now);
        return taskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<Task> list(
            AuthenticatedUser requester,
            Optional<UUID> userId,
            Optional<TaskStatus> status,
            boolean overdueOnly,
            int page,
            int size) {
        var effectiveUserId = requester.isPrivileged() ? userId : Optional.of(requester.userId());
        return taskRepository.findFiltered(
                effectiveUserId, status, overdueOnly, OffsetDateTime.now(), page, size);
    }

    @Transactional(readOnly = true)
    public Optional<Task> findById(UUID id) {
        return taskRepository.findById(id);
    }

    @Transactional
    public Optional<Task> update(AuthenticatedUser requester, UUID id, UpdateTaskCommand command) {
        return taskRepository
                .findByIdForUpdate(id)
                .map(
                        existing -> {
                            validateCanMutate(requester, existing);
                            var nextProjectId =
                                    command.projectId() != null
                                            ? command.projectId()
                                            : existing.projectId();
                            projectMembershipApplicationService.validateMembership(
                                    nextProjectId, existing.userId());

                            var updated =
                                    new Task(
                                            existing.id(),
                                            existing.version(),
                                            existing.userId(),
                                            nextProjectId,
                                            command.title() != null
                                                    ? command.title().trim()
                                                    : existing.title(),
                                            command.description() != null
                                                    ? command.description()
                                                    : existing.description(),
                                            command.status() != null
                                                    ? command.status()
                                                    : existing.status(),
                                            command.priority() != null
                                                    ? command.priority()
                                                    : existing.priority(),
                                            command.dueAt() != null
                                                    ? command.dueAt()
                                                    : existing.dueAt(),
                                            command.durationMinutes() != null
                                                    ? command.durationMinutes()
                                                    : existing.durationMinutes(),
                                            command.energyLevel() != null
                                                    ? command.energyLevel()
                                                    : existing.energyLevel(),
                                            existing.source(),
                                            existing.confidence(),
                                            existing.createdAt(),
                                            Instant.now());
                            return taskRepository.save(updated);
                        });
    }

    @Transactional
    public Optional<Task> updateStatus(AuthenticatedUser requester, UUID id, TaskStatus status) {
        return taskRepository
                .findByIdForUpdate(id)
                .map(
                        existing -> {
                            validateCanMutate(requester, existing);
                            return taskRepository.save(existing.withStatus(status, Instant.now()));
                        });
    }

    @Transactional
    public Optional<Task> archive(AuthenticatedUser requester, UUID id) {
        return updateStatus(requester, id, TaskStatus.ARCHIVED);
    }

    private void validateCanMutate(AuthenticatedUser requester, Task task) {
        if (!requester.isPrivileged() && !requester.userId().equals(task.userId())) {
            throw new IllegalArgumentException("Cannot mutate another user's task");
        }
    }
}
