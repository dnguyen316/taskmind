package com.taskmind.backend.task.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.events.TaskDomainEventPublisher;
import com.taskmind.backend.project.application.ProjectMembershipApplicationService;
import com.taskmind.backend.task.domain.*;
import com.taskmind.backend.task.domain.model.*;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskApplicationService {
    private final TaskRepository tasks;
    private final ProjectMembershipApplicationService memberships;
    private final TaskKeyAssigner keys;
    private final TaskDomainEventPublisher events;

    public TaskApplicationService(
            TaskRepository tasks,
            ProjectMembershipApplicationService memberships,
            TaskKeyAssigner keys,
            TaskDomainEventPublisher events) {
        this.tasks = tasks;
        this.memberships = memberships;
        this.keys = keys;
        this.events = events;
    }

    @Transactional
    public Task create(AuthenticatedUser requester, CreateTaskCommand c) {
        UUID owner = requester.isPrivileged() ? c.userId() : requester.userId();
        memberships.validateMembership(c.projectId(), owner);
        if (c.assigneeId() != null) memberships.validateMembership(c.projectId(), c.assigneeId());
        TaskLevel level = c.taskLevel() == null ? TaskLevel.TASK : c.taskLevel();
        TaskType type = c.taskType() == null ? TaskType.TASK : c.taskType();
        TaskTypeRules.validate(type, level);
        Instant now = Instant.now();
        Task task =
                new Task(
                        UUID.randomUUID(),
                        null,
                        owner,
                        c.projectId(),
                        keys.assign(c.projectId()),
                        c.assigneeId(),
                        c.parentTaskId(),
                        level,
                        type,
                        c.storyPoints(),
                        c.releaseVersion(),
                        null,
                        c.title().trim(),
                        c.description(),
                        c.status(),
                        c.priority(),
                        c.dueAt(),
                        c.durationMinutes(),
                        c.energyLevel(),
                        c.source(),
                        c.confidence(),
                        now,
                        now);
        validateParent(task);
        Task saved = tasks.save(task);
        events.taskCreated(saved, requester.userId());
        return saved;
    }

    public List<Task> list(
            AuthenticatedUser r,
            Optional<UUID> u,
            Optional<TaskStatus> s,
            boolean o,
            int p,
            int z) {
        return tasks.findFiltered(
                r.isPrivileged() ? u : Optional.of(r.userId()), s, o, OffsetDateTime.now(), p, z);
    }

    public Optional<Task> findById(UUID id) {
        return tasks.findById(id);
    }

    public Optional<Task> findById(AuthenticatedUser r, UUID id) {
        return tasks.findById(id).filter(t -> canRead(r, t));
    }

    public List<Task> children(AuthenticatedUser r, UUID id) {
        Task root = authorized(r, id);
        return tasks.findChildren(root.id());
    }

    public List<Task> ancestors(AuthenticatedUser r, UUID id) {
        authorized(r, id);
        return tasks.findAncestors(id);
    }

    @Transactional
    public Optional<Task> update(AuthenticatedUser r, UUID id, UpdateTaskCommand c) {
        return tasks.findByIdForUpdate(id)
                .map(
                        e -> {
                            validateCanMutate(r, e);
                            if (c.version() != null && !c.version().equals(e.version()))
                                throw new org.springframework.orm
                                        .ObjectOptimisticLockingFailureException(Task.class, id);
                            UUID project = c.projectId() != null ? c.projectId() : e.projectId();
                            memberships.validateMembership(project, e.userId());
                            if (c.assigneeId() != null)
                                memberships.validateMembership(project, c.assigneeId());
                            Task updated =
                                    new Task(
                                            e.id(),
                                            e.version(),
                                            e.userId(),
                                            project,
                                            e.taskKey(),
                                            c.assigneeId() != null
                                                    ? c.assigneeId()
                                                    : e.assigneeId(),
                                            c.parentTaskId() != null
                                                    ? c.parentTaskId()
                                                    : e.parentTaskId(),
                                            c.taskLevel() != null ? c.taskLevel() : e.taskLevel(),
                                            c.taskType() != null ? c.taskType() : e.taskType(),
                                            c.storyPoints() != null
                                                    ? c.storyPoints()
                                                    : e.storyPoints(),
                                            c.releaseVersion() != null
                                                    ? c.releaseVersion()
                                                    : e.releaseVersion(),
                                            e.deletedAt(),
                                            c.title() != null ? c.title().trim() : e.title(),
                                            c.description() != null
                                                    ? c.description()
                                                    : e.description(),
                                            c.status() != null ? c.status() : e.status(),
                                            c.priority() != null ? c.priority() : e.priority(),
                                            c.dueAt() != null ? c.dueAt() : e.dueAt(),
                                            c.durationMinutes() != null
                                                    ? c.durationMinutes()
                                                    : e.durationMinutes(),
                                            c.energyLevel() != null
                                                    ? c.energyLevel()
                                                    : e.energyLevel(),
                                            e.source(),
                                            e.confidence(),
                                            e.createdAt(),
                                            Instant.now());
                            validateParent(updated);
                            Task saved = tasks.save(updated);
                            events.taskUpdated(e, saved, r.userId());
                            return saved;
                        });
    }

    @Transactional
    public Optional<Task> updateStatus(AuthenticatedUser r, UUID id, TaskStatus s) {
        return tasks.findByIdForUpdate(id)
                .map(
                        e -> {
                            validateCanMutate(r, e);
                            Task saved = tasks.save(e.withStatus(s, Instant.now()));
                            events.taskUpdated(e, saved, r.userId());
                            return saved;
                        });
    }

    @Transactional
    public Optional<Task> archive(AuthenticatedUser r, UUID id) {
        return updateStatus(r, id, TaskStatus.ARCHIVED);
    }

    private void validateParent(Task t) {
        if (t.parentTaskId() != null) {
            Task p =
                    tasks.findById(t.parentTaskId())
                            .orElseThrow(
                                    () -> new IllegalArgumentException("Parent task not found"));
            TaskHierarchyRules.validateParent(t, p, tasks.findAncestors(p.id()));
        }
    }

    private Task authorized(AuthenticatedUser r, UUID id) {
        return findById(r, id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found or access denied"));
    }

    private boolean canRead(AuthenticatedUser r, Task t) {
        return r.isPrivileged()
                || r.userId().equals(t.userId())
                || (t.projectId() != null && memberships.isMember(t.projectId(), r.userId()));
    }

    private void validateCanMutate(AuthenticatedUser r, Task t) {
        if (!r.isPrivileged() && !r.userId().equals(t.userId()))
            throw new IllegalArgumentException("Cannot modify another user's task");
    }
}
