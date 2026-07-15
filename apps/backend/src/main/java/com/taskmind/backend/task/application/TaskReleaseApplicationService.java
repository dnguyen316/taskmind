package com.taskmind.backend.task.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.project.application.ProjectMembershipApplicationService;
import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.backend.project.domain.repository.ProjectRepository;
import com.taskmind.backend.task.domain.repository.TaskRepository;
import com.taskmind.backend.task.interfaces.rest.dto.*;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TaskReleaseApplicationService {
    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final ProjectMembershipApplicationService members;

    public TaskReleaseApplicationService(
            TaskRepository t, ProjectRepository p, ProjectMembershipApplicationService m) {
        tasks = t;
        projects = p;
        members = m;
    }

    public ReleaseSummaryResponse summary(AuthenticatedUser user, UUID projectId) {
        Project p = projects.findById(projectId)
                .orElseThrow(() -> new TaskNotFoundException(
                        "Project not found",
                        new TaskErrorMetadata(
                                TaskErrorCode.TASK_NOT_FOUND,
                                "project",
                                projectId.toString(),
                                "release-summary",
                                null,
                                null)));
        if (!user.isPrivileged()
                && !p.ownerUserId().equals(user.userId())
                && !members.isMember(projectId, user.userId()))
            throw new TaskAccessDeniedException(
                    "Project access denied",
                    new TaskErrorMetadata(
                            TaskErrorCode.TASK_ACCESS_DENIED,
                            "project",
                            projectId.toString(),
                            "release-summary",
                            null,
                            null));
        return new ReleaseSummaryResponse(
                projectId,
                tasks.releaseStats(projectId).stream()
                        .map(
                                s ->
                                        new ProjectReleaseResponse(
                                                s.getReleaseVersion(),
                                                s.getTotalTasks(),
                                                s.getCompletedTasks(),
                                                s.getTotalStoryPoints() == null
                                                        ? 0
                                                        : s.getTotalStoryPoints(),
                                                s.getCompletedStoryPoints() == null
                                                        ? 0
                                                        : s.getCompletedStoryPoints()))
                        .toList());
    }
}
