package com.taskmind.backend.task.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.task.application.TaskReleaseApplicationService;
import com.taskmind.backend.task.interfaces.rest.dto.ReleaseSummaryResponse;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/projects/{projectId}/releases")
public class TaskReleaseController {

    private final TaskReleaseApplicationService taskReleaseApplicationService;

    public TaskReleaseController(TaskReleaseApplicationService taskReleaseApplicationService) {
        this.taskReleaseApplicationService = taskReleaseApplicationService;
    }

    @GetMapping
    public ReleaseSummaryResponse summary(AuthenticatedUser requester, @PathVariable UUID projectId) {
        return taskReleaseApplicationService.summary(requester, projectId);
    }
}
