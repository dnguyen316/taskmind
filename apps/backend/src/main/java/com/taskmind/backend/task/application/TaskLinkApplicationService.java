package com.taskmind.backend.task.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.project.application.ProjectMembershipApplicationService;
import com.taskmind.backend.task.domain.model.*;
import com.taskmind.backend.task.domain.repository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskLinkApplicationService {
    private final TaskRepository tasks;
    private final TaskLinkRepository links;
    private final ProjectMembershipApplicationService memberships;

    public TaskLinkApplicationService(
            TaskRepository tasks,
            TaskLinkRepository links,
            ProjectMembershipApplicationService memberships) {
        this.tasks = tasks;
        this.links = links;
        this.memberships = memberships;
    }

    @Transactional
    public TaskLink create(
            AuthenticatedUser authenticatedUser, UUID source, UUID target, TaskLinkType type) {
        Task sourceTask = authorizedToMutateLink(authenticatedUser, source);
        Task targetTask = authorizedToMutateLink(authenticatedUser, target);
        if (sourceTask.projectId() == null
                || !sourceTask.projectId().equals(targetTask.projectId()))
            throw new TaskValidationException("Linked tasks must belong to the same project");
        return links.save(
                new TaskLink(
                        UUID.randomUUID(),
                        null,
                        source,
                        target,
                        type,
                        authenticatedUser.userId(),
                        Instant.now()));
    }

    public List<TaskLink> list(AuthenticatedUser authenticatedUser, UUID id) {
        authorizedToRead(authenticatedUser, id);
        return links.findForTask(id);
    }

    @Transactional
    public void delete(AuthenticatedUser authenticatedUser, UUID id) {
        TaskLink link =
                links.findById(id)
                        .orElseThrow(() -> new TaskNotFoundException("Task link not found"));
        authorizedToMutateLink(authenticatedUser, link.sourceTaskId());
        links.deleteById(id);
    }

    private Task authorizedToRead(AuthenticatedUser authenticatedUser, UUID id) {
        Task task =
                tasks.findById(id).orElseThrow(() -> new TaskNotFoundException("Task not found"));
        if (!canRead(authenticatedUser, task)) throw new TaskNotFoundException("Task not found");
        return task;
    }

    private Task authorizedToMutateLink(AuthenticatedUser authenticatedUser, UUID id) {
        Task task =
                tasks.findById(id).orElseThrow(() -> new TaskNotFoundException("Task not found"));
        if (!canMutateLink(authenticatedUser, task))
            throw new TaskNotFoundException("Task not found");
        return task;
    }

    private boolean canRead(AuthenticatedUser authenticatedUser, Task task) {
        return authenticatedUser.isPrivileged()
                || authenticatedUser.userId().equals(task.userId())
                || (task.projectId() != null
                        && memberships.isMember(task.projectId(), authenticatedUser.userId()));
    }

    private boolean canMutateLink(AuthenticatedUser authenticatedUser, Task task) {
        return authenticatedUser.isPrivileged()
                || authenticatedUser.userId().equals(task.userId())
                || (task.projectId() != null
                        && memberships.isMember(task.projectId(), authenticatedUser.userId()));
    }
}
