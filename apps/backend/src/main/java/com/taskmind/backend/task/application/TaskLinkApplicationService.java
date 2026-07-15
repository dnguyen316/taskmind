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
            TaskRepository t, TaskLinkRepository l, ProjectMembershipApplicationService m) {
        tasks = t;
        links = l;
        memberships = m;
    }

    @Transactional
    public TaskLink create(AuthenticatedUser u, UUID source, UUID target, TaskLinkType type) {
        Task s = authorizedToMutateLink(u, source);
        Task t = authorizedToMutateLink(u, target);
        if (s.projectId() == null || !s.projectId().equals(t.projectId()))
            throw new TaskValidationException("Linked tasks must belong to the same project");
        return links.save(
                new TaskLink(
                        UUID.randomUUID(), null, source, target, type, u.userId(), Instant.now()));
    }

    public List<TaskLink> list(AuthenticatedUser u, UUID id) {
        authorizedToRead(u, id);
        return links.findForTask(id);
    }

    @Transactional
    public void delete(AuthenticatedUser u, UUID id) {
        TaskLink l = links.findById(id).orElseThrow(() -> new TaskNotFoundException("Task link not found"));
        authorizedToMutateLink(u, l.sourceTaskId());
        links.deleteById(id);
    }

    private Task authorizedToRead(AuthenticatedUser u, UUID id) {
        Task t = tasks.findById(id).orElseThrow(() -> new TaskNotFoundException("Task not found"));
        if (!canRead(u, t)) throw new TaskNotFoundException("Task not found");
        return t;
    }

    private Task authorizedToMutateLink(AuthenticatedUser u, UUID id) {
        Task t = tasks.findById(id).orElseThrow(() -> new TaskNotFoundException("Task not found"));
        if (!canMutateLink(u, t)) throw new TaskNotFoundException("Task not found");
        return t;
    }

    private boolean canRead(AuthenticatedUser u, Task t) {
        return u.isPrivileged()
                || u.userId().equals(t.userId())
                || (t.projectId() != null && memberships.isMember(t.projectId(), u.userId()));
    }

    private boolean canMutateLink(AuthenticatedUser u, Task t) {
        return u.isPrivileged()
                || u.userId().equals(t.userId())
                || (t.projectId() != null && memberships.isMember(t.projectId(), u.userId()));
    }
}
