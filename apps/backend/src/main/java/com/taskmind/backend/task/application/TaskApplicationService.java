package com.taskmind.backend.task.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.events.TaskDomainEventPublisher;
import com.taskmind.backend.project.application.ProjectMembershipApplicationService;
import com.taskmind.backend.task.domain.*;
import com.taskmind.backend.task.domain.model.*;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import com.taskmind.backend.tasktype.application.TaskTypeApplicationService;
import com.taskmind.backend.tasktype.domain.model.TaskTypeDefinition;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskApplicationService {
    private final TaskRepository tasks;
    private final ProjectMembershipApplicationService memberships;
    private final TaskKeyAssigner keys;
    private final TaskDomainEventPublisher events;
    private final TaskTypeApplicationService taskTypes;


    public TaskApplicationService(
            TaskRepository tasks,
            ProjectMembershipApplicationService memberships,
            TaskKeyAssigner keys,
            TaskDomainEventPublisher events) {
        this(tasks, memberships, keys, events, null);
    }

    @Autowired
    public TaskApplicationService(
            TaskRepository tasks,
            ProjectMembershipApplicationService memberships,
            TaskKeyAssigner keys,
            TaskDomainEventPublisher events,
            TaskTypeApplicationService taskTypes) {
        this.tasks = tasks;
        this.memberships = memberships;
        this.keys = keys;
        this.events = events;
        this.taskTypes = taskTypes;
    }

    @Transactional
    public Task create(AuthenticatedUser requester, CreateTaskCommand c) {
        UUID owner = requester.isPrivileged() ? c.userId() : requester.userId();
        validateMembership(c.projectId(), owner);
        if (c.assigneeId() != null) validateMembership(c.projectId(), c.assigneeId());
        String requestedType = c.taskType() == null ? "TASK" : c.taskType();
        TaskTypeDefinition typeDefinition = resolveActiveType(c.projectId(), requestedType);
        TaskLevel level = c.taskLevel() == null ? typeDefinition.defaultTaskLevel() : c.taskLevel();
        validateTaskType(typeDefinition, level);
        String type = typeDefinition.key();
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
        return list(
                r,
                new TaskQuery(
                        (r.isPrivileged() ? u : Optional.of(r.userId())).orElse(null),
                        s.orElse(null),
                        false,
                        o,
                        false,
                        false,
                        false,
                        false,
                        false,
                        null,
                        null,
                        null,
                        "updatedAt",
                        p,
                        z));
    }

    public List<Task> list(AuthenticatedUser r, TaskQuery q) {
        UUID user = r.isPrivileged() ? q.userId() : r.userId();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime todayStart = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime tomorrowStart = todayStart.plusDays(1);
        Instant staleBefore = now.toInstant().minus(Duration.ofDays(14));
        return tasks.findFiltered(
                new TaskQuery(
                        user,
                        q.status(),
                        q.dueToday(),
                        q.overdue(),
                        q.blocked(),
                        q.unassigned(),
                        q.noDueDate(),
                        q.stale(),
                        q.archived(),
                        q.priority(),
                        q.projectId(),
                        q.assigneeId(),
                        q.sort(),
                        q.page(),
                        q.size()),
                now,
                todayStart,
                tomorrowStart,
                staleBefore);
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
                            validateMembership(project, e.userId());
                            if (c.assigneeId() != null) validateMembership(project, c.assigneeId());
                            TaskTypeDefinition typeDefinition = resolveActiveType(project, c.taskType() != null ? c.taskType() : e.taskType());
                            TaskLevel resolvedLevel = c.taskLevel() != null ? c.taskLevel() : e.taskLevel();
                            validateTaskType(typeDefinition, resolvedLevel);
                            String resolvedType = typeDefinition.key();
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
                                            resolvedLevel,
                                            resolvedType,
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
    public Optional<Task> updateStatus(AuthenticatedUser r, UUID id, TaskStatus s, Long version) {
        return tasks.findByIdForUpdate(id)
                .map(
                        e -> {
                            validateCanMutate(r, e);
                            if (version != null && !version.equals(e.version()))
                                throw new org.springframework.orm
                                        .ObjectOptimisticLockingFailureException(Task.class, id);
                            Task saved = tasks.save(e.withStatus(s, Instant.now()));
                            events.taskUpdated(e, saved, r.userId());
                            return saved;
                        });
    }

    @Transactional
    public Optional<Task> archive(AuthenticatedUser r, UUID id) {
        return updateStatus(r, id, TaskStatus.ARCHIVED, null);
    }

    private TaskTypeDefinition resolveActiveType(UUID projectId, String key) {
        return taskTypes != null ? taskTypes.requireActiveByKey(projectId, key) : TaskTypeRules.systemDefinition(key);
    }

    private void validateParent(Task t) {
        if (t.parentTaskId() != null) {
            Task p =
                    tasks.findById(t.parentTaskId())
                            .orElseThrow(
                                    () -> new TaskNotFoundException("Parent task not found"));
            try {
                TaskHierarchyRules.validateParent(t, p, tasks.findAncestors(p.id()));
            } catch (IllegalArgumentException e) {
                throw new TaskValidationException(e.getMessage(), e);
            }
        }
    }

    private Task authorized(AuthenticatedUser r, UUID id) {
        return tasks.findById(id)
                .map(
                        task -> {
                            if (!canRead(r, task)) {
                                throw new TaskAccessDeniedException("Task access denied");
                            }
                            return task;
                        })
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));
    }

    private boolean canRead(AuthenticatedUser r, Task t) {
        return r.isPrivileged()
                || r.userId().equals(t.userId())
                || (t.projectId() != null && memberships.isMember(t.projectId(), r.userId()));
    }

    private void validateCanMutate(AuthenticatedUser r, Task t) {
        if (!r.isPrivileged() && !r.userId().equals(t.userId()))
            throw new TaskAccessDeniedException("Cannot modify another user's task");
    }

    private void validateTaskType(TaskTypeDefinition definition, TaskLevel level) {
        try {
            TaskTypeRules.validate(definition, level);
        } catch (IllegalArgumentException e) {
            throw new TaskValidationException(e.getMessage(), e);
        }
    }

    private void validateMembership(UUID projectId, UUID userId) {
        try {
            memberships.validateMembership(projectId, userId);
        } catch (IllegalArgumentException e) {
            throw new TaskAccessDeniedException(e.getMessage());
        }
    }
}
