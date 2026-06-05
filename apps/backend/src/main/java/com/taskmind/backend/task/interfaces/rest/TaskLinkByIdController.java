package com.taskmind.backend.task.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.task.application.TaskLinkApplicationService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/task-links")
public class TaskLinkByIdController {

    private final TaskLinkApplicationService taskLinkApplicationService;

    public TaskLinkByIdController(TaskLinkApplicationService taskLinkApplicationService) {
        this.taskLinkApplicationService = taskLinkApplicationService;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(AuthenticatedUser requester, @PathVariable UUID id) {
        taskLinkApplicationService.delete(requester, id);
        return ResponseEntity.noContent().build();
    }
}
