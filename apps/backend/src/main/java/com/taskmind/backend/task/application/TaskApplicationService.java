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
    public Task create(AuthenticatedUser requester, CreateTaskCommand command) {
        UUID owner = requester.isPrivileged() ? command.userId() : requester.userId();
        validateMembership(command.projectId(), owner);
        if (command.assigneeId() != null)
            validateMembership(command.projectId(), command.assigneeId());
        String requestedType = command.taskType() == null ? "TASK" : command.taskType();
        TaskTypeDefinition typeDefinition = resolveActiveType(command.projectId(), requestedType);
        TaskLevel level =
                command.taskLevel() == null
                        ? typeDefinition.defaultTaskLevel()
                        : command.taskLevel();
        validateTaskType(typeDefinition, level);
        String type = typeDefinition.key();
        Instant now = Instant.now();
        Task task =
                new Task(
                        UUID.randomUUID(),
                        null,
                        owner,
                        command.projectId(),
                        keys.assign(command.projectId()),
                        command.assigneeId(),
                        command.parentTaskId(),
                        level,
                        type,
                        command.storyPoints(),
                        command.releaseVersion(),
                        null,
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
        validateParent(task);
        Task saved = tasks.save(task);
        events.taskCreated(saved, requester.userId());
        return saved;
    }

    public List<Task> list(
            AuthenticatedUser requester,
            Optional<UUID> user,
            Optional<TaskStatus> status,
            boolean overdue,
            int page,
            int size) {
        return list(
                requester,
                new TaskQuery(
                        (requester.isPrivileged() ? user : Optional.of(requester.userId()))
                                .orElse(null),
                        status.orElse(null),
                        false,
                        overdue,
                        false,
                        false,
                        false,
                        false,
                        false,
                        null,
                        null,
                        null,
                        "updatedAt",
                        page,
                        size));
    }

    public List<Task> list(AuthenticatedUser requester, TaskQuery query) {
        UUID user = requester.isPrivileged() ? query.userId() : requester.userId();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime todayStart = now.toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime tomorrowStart = todayStart.plusDays(1);
        Instant staleBefore = now.toInstant().minus(Duration.ofDays(14));
        return tasks.findFiltered(
                new TaskQuery(
                        user,
                        query.status(),
                        query.dueToday(),
                        query.overdue(),
                        query.blocked(),
                        query.unassigned(),
                        query.noDueDate(),
                        query.stale(),
                        query.archived(),
                        query.priority(),
                        query.projectId(),
                        query.assigneeId(),
                        query.sort(),
                        query.page(),
                        query.size()),
                now,
                todayStart,
                tomorrowStart,
                staleBefore);
    }

    public Optional<Task> findById(UUID id) {
        return tasks.findById(id);
    }

    public Optional<Task> findById(AuthenticatedUser requester, UUID id) {
        return tasks.findById(id).map(task -> requireReadable(requester, task));
    }

    public List<Task> children(AuthenticatedUser requester, UUID id) {
        Task root = authorized(requester, id);
        return tasks.findChildren(root.id());
    }

    public List<Task> ancestors(AuthenticatedUser requester, UUID id) {
        authorized(requester, id);
        return tasks.findAncestors(id);
    }

    @Transactional
    public Optional<Task> update(AuthenticatedUser requester, UUID id, UpdateTaskCommand command) {
        return tasks.findByIdForUpdate(id)
                .map(
                        entity -> {
                            validateCanMutate(requester, entity);
                            if (command.version() != null
                                    && !command.version().equals(entity.version()))
                                throw new org.springframework.orm
                                        .ObjectOptimisticLockingFailureException(Task.class, id);
                            UUID project =
                                    command.projectId() != null
                                            ? command.projectId()
                                            : entity.projectId();
                            validateMembership(project, entity.userId());
                            if (command.assigneeId() != null)
                                validateMembership(project, command.assigneeId());
                            TaskTypeDefinition typeDefinition =
                                    resolveActiveType(
                                            project,
                                            command.taskType() != null
                                                    ? command.taskType()
                                                    : entity.taskType());
                            TaskLevel resolvedLevel =
                                    command.taskLevel() != null
                                            ? command.taskLevel()
                                            : entity.taskLevel();
                            validateTaskType(typeDefinition, resolvedLevel);
                            String resolvedType = typeDefinition.key();
                            Task updated =
                                    new Task(
                                            entity.id(),
                                            entity.version(),
                                            entity.userId(),
                                            project,
                                            entity.taskKey(),
                                            command.assigneeId() != null
                                                    ? command.assigneeId()
                                                    : entity.assigneeId(),
                                            command.parentTaskId() != null
                                                    ? command.parentTaskId()
                                                    : entity.parentTaskId(),
                                            resolvedLevel,
                                            resolvedType,
                                            command.storyPoints() != null
                                                    ? command.storyPoints()
                                                    : entity.storyPoints(),
                                            command.releaseVersion() != null
                                                    ? command.releaseVersion()
                                                    : entity.releaseVersion(),
                                            entity.deletedAt(),
                                            command.title() != null
                                                    ? command.title().trim()
                                                    : entity.title(),
                                            command.description() != null
                                                    ? command.description()
                                                    : entity.description(),
                                            command.status() != null
                                                    ? command.status()
                                                    : entity.status(),
                                            command.priority() != null
                                                    ? command.priority()
                                                    : entity.priority(),
                                            command.dueAt() != null
                                                    ? command.dueAt()
                                                    : entity.dueAt(),
                                            command.durationMinutes() != null
                                                    ? command.durationMinutes()
                                                    : entity.durationMinutes(),
                                            command.energyLevel() != null
                                                    ? command.energyLevel()
                                                    : entity.energyLevel(),
                                            entity.source(),
                                            entity.confidence(),
                                            entity.createdAt(),
                                            Instant.now());
                            validateParent(updated);
                            Task saved = tasks.save(updated);
                            events.taskUpdated(entity, saved, requester.userId());
                            return saved;
                        });
    }

    @Transactional
    public Optional<Task> updateStatus(
            AuthenticatedUser requester, UUID id, TaskStatus status, Long version) {
        return tasks.findByIdForUpdate(id)
                .map(
                        entity -> {
                            validateCanMutate(requester, entity);
                            if (version != null && !version.equals(entity.version()))
                                throw new org.springframework.orm
                                        .ObjectOptimisticLockingFailureException(Task.class, id);
                            Task saved = tasks.save(entity.withStatus(status, Instant.now()));
                            events.taskUpdated(entity, saved, requester.userId());
                            return saved;
                        });
    }

    @Transactional
    public Optional<Task> archive(AuthenticatedUser requester, UUID id) {
        return updateStatus(requester, id, TaskStatus.ARCHIVED, null);
    }

    private TaskTypeDefinition resolveActiveType(UUID projectId, String key) {
        return taskTypes != null
                ? taskTypes.requireActiveByKey(projectId, key)
                : TaskTypeRules.systemDefinition(key);
    }

    private void validateParent(Task task) {
        if (task.parentTaskId() != null) {
            Task parent =
                    tasks.findById(task.parentTaskId())
                            .orElseThrow(() -> new TaskNotFoundException("Parent task not found"));
            try {
                TaskHierarchyRules.validateParent(task, parent, tasks.findAncestors(parent.id()));
            } catch (IllegalArgumentException exception) {
                throw new TaskValidationException(
                        exception.getMessage(),
                        exception,
                        taskValidationReason(hierarchyReason(exception.getMessage())));
            }
        }
    }

    private Task authorized(AuthenticatedUser requester, UUID id) {
        return tasks.findById(id)
                .map(task -> requireReadable(requester, task))
                .orElseThrow(() -> new TaskNotFoundException("Task not found"));
    }

    private Task requireReadable(AuthenticatedUser requester, Task task) {
        if (!canRead(requester, task)) {
            throw new TaskNotFoundException("Task not found");
        }
        return task;
    }

    private boolean canRead(AuthenticatedUser requester, Task task) {
        return requester.isPrivileged()
                || requester.userId().equals(task.userId())
                || (task.projectId() != null
                        && memberships.isMember(task.projectId(), requester.userId()));
    }

    private void validateCanMutate(AuthenticatedUser requester, Task task) {
        if (!requester.isPrivileged() && !requester.userId().equals(task.userId()))
            throw new TaskNotFoundException("Task not found");
    }

    private void validateTaskType(TaskTypeDefinition definition, TaskLevel level) {
        try {
            TaskTypeRules.validate(definition, level);
        } catch (IllegalArgumentException exception) {
            throw new TaskValidationException(
                    exception.getMessage(),
                    exception,
                    taskValidationReason(taskTypeReason(exception.getMessage())));
        }
    }

    private TaskErrorMetadata taskValidationReason(String reason) {
        return new TaskErrorMetadata(
                TaskErrorCode.TASK_VALIDATION_FAILED, null, null, null, null, reason);
    }

    private String hierarchyReason(String message) {
        return switch (message) {
            case "A task cannot be its own parent" -> "TASK_PARENT_SAME_AS_CHILD";
            case "Parent and child must belong to the same project" ->
                    "TASK_PARENT_PROJECT_MISMATCH";
            case "Task hierarchy cannot contain a cycle" -> "TASK_HIERARCHY_CYCLE";
            case "Task hierarchy exceeds maximum depth" -> "TASK_HIERARCHY_MAX_DEPTH";
            case "Invalid task hierarchy level" -> "TASK_HIERARCHY_INVALID_LEVEL";
            default -> null;
        };
    }

    private String taskTypeReason(String message) {
        return switch (message) {
            case "Task type is required" -> "TASK_TYPE_REQUIRED";
            case "Unknown task type" -> "TASK_TYPE_UNKNOWN";
            case "Task type and level are required" -> "TASK_TYPE_LEVEL_REQUIRED";
            case "Task type is not valid for its hierarchy level" -> "TASK_TYPE_INVALID_FOR_LEVEL";
            default -> null;
        };
    }

    private void validateMembership(UUID projectId, UUID userId) {
        try {
            memberships.validateMembership(projectId, userId);
        } catch (IllegalArgumentException exception) {
            throw new TaskAccessDeniedException(exception.getMessage());
        }
    }
}
