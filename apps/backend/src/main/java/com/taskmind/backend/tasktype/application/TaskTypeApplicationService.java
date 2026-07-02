package com.taskmind.backend.tasktype.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.project.domain.model.ProjectMembership;
import com.taskmind.backend.project.domain.model.ProjectMembershipRole;
import com.taskmind.backend.project.domain.repository.ProjectMembershipRepository;
import com.taskmind.backend.project.domain.repository.ProjectRepository;
import com.taskmind.backend.task.domain.model.TaskLevel;
import com.taskmind.backend.tasktype.domain.model.TaskTypeDefinition;
import com.taskmind.backend.tasktype.domain.repository.TaskTypeRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TaskTypeApplicationService {
    private final TaskTypeRepository taskTypes;
    private final ProjectRepository projects;
    private final ProjectMembershipRepository memberships;

    public TaskTypeApplicationService(
            TaskTypeRepository taskTypes,
            ProjectRepository projects,
            ProjectMembershipRepository memberships) {
        this.taskTypes = taskTypes;
        this.projects = projects;
        this.memberships = memberships;
    }

    public List<TaskTypeDefinition> listActive(AuthenticatedUser actor, UUID projectId) {
        if (projectId == null || canReadProjectTypes(actor, projectId)) {
            return taskTypes.findActive(projectId);
        }
        return taskTypes.findActive(null);
    }

    public TaskTypeDefinition requireActiveByKey(UUID projectId, String key) {
        return taskTypes.findActiveByKey(projectId, normalize(key))
                .orElseThrow(() -> new IllegalArgumentException("Unknown task type"));
    }

    @Transactional
    public TaskTypeDefinition create(
            AuthenticatedUser actor,
            UUID projectId,
            String key,
            String name,
            String color,
            String icon,
            Integer sortOrder) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project id is required");
        }
        assertCanManageProjectTypes(actor, projectId);
        Instant now = Instant.now();
        return taskTypes.save(new TaskTypeDefinition(UUID.randomUUID(), null, projectId, normalize(key), name, color, icon, TaskLevel.TASK, Set.of(TaskLevel.TASK), false, false, null, false, true, sortOrder, now, now));
    }

    @Transactional
    public Optional<TaskTypeDefinition> update(
            AuthenticatedUser actor,
            UUID id,
            String name,
            Long version,
            String color,
            String icon,
            Boolean active,
            Integer sortOrder) {
        return taskTypes.findById(id).map(existing -> {
            assertCanManageTaskType(actor, existing);
            if (version != null && !version.equals(existing.version())) {
                throw new ObjectOptimisticLockingFailureException(TaskTypeDefinition.class, id);
            }
            return taskTypes.save(new TaskTypeDefinition(
                    existing.id(), existing.version(), existing.projectId(), existing.key(),
                    name != null ? name : existing.name(),
                    color != null ? color : existing.color(),
                    icon != null ? icon : existing.icon(),
                    existing.defaultTaskLevel(), existing.allowedTaskLevels(), existing.container(), existing.allowChildren(), existing.systemKind(),
                    existing.system(), active != null ? active : existing.active(),
                    sortOrder != null ? sortOrder : existing.sortOrder(), existing.createdAt(), Instant.now()));
        });
    }

    private void assertCanManageTaskType(AuthenticatedUser actor, TaskTypeDefinition taskType) {
        if (taskType.projectId() == null) {
            if (!actor.isPrivileged()) {
                throw new TaskTypeForbiddenException("Actor is not allowed to manage global task types");
            }
            return;
        }
        assertCanManageProjectTypes(actor, taskType.projectId());
    }

    private void assertCanManageProjectTypes(AuthenticatedUser actor, UUID projectId) {
        if (!canManageProjectTypes(actor, projectId)) {
            throw new TaskTypeForbiddenException("Actor is not allowed to manage project task types");
        }
    }

    private boolean canReadProjectTypes(AuthenticatedUser actor, UUID projectId) {
        return actor.isPrivileged()
                || projects.findById(projectId)
                        .map(project -> project.ownerUserId().equals(actor.userId()))
                        .orElse(false)
                || memberships.existsByProjectIdAndUserId(projectId, actor.userId());
    }

    private boolean canManageProjectTypes(AuthenticatedUser actor, UUID projectId) {
        if (actor.isPrivileged()) {
            return projects.findById(projectId).isPresent();
        }
        if (projects.findById(projectId)
                .map(project -> project.ownerUserId().equals(actor.userId()))
                .orElse(false)) {
            return true;
        }
        return memberships
                .findByProjectIdAndUserId(projectId, actor.userId())
                .map(ProjectMembership::role)
                .map(role -> role == ProjectMembershipRole.ADMIN || role == ProjectMembershipRole.OWNER)
                .orElse(false);
    }

    private String normalize(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Task type key is required");
        return key.trim().toUpperCase();
    }
}
