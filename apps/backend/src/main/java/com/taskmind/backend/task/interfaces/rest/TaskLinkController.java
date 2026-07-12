package com.taskmind.backend.task.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.task.application.TaskLinkApplicationService;
import com.taskmind.backend.task.domain.model.TaskLink;
import com.taskmind.backend.task.interfaces.rest.dto.CreateTaskLinkRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/tasks/{taskId}/links")
public class TaskLinkController {

    private final TaskLinkApplicationService taskLinkApplicationService;

    public TaskLinkController(TaskLinkApplicationService taskLinkApplicationService) {
        this.taskLinkApplicationService = taskLinkApplicationService;
    }

    @GetMapping
    public List<TaskLink> list(AuthenticatedUser requester, @PathVariable UUID taskId) {
        return taskLinkApplicationService.list(requester, taskId);
    }

    @PostMapping
    public ResponseEntity<TaskLink> create(
        AuthenticatedUser requester,
        @PathVariable UUID taskId,
        @Valid @RequestBody CreateTaskLinkRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        taskLinkApplicationService.create(
                                requester, taskId, request.targetTaskId(), request.linkType()));
    }
}
