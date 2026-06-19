package com.taskmind.backend.task.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.task.application.CreateTaskCommand;
import com.taskmind.backend.task.application.TaskApplicationService;
import com.taskmind.backend.task.application.UpdateTaskCommand;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.interfaces.rest.dto.CreateTaskRequest;
import com.taskmind.backend.task.interfaces.rest.dto.TaskCompletionResponse;
import com.taskmind.backend.task.interfaces.rest.dto.TaskResponse;
import com.taskmind.backend.task.interfaces.rest.dto.UpdateTaskRequest;
import com.taskmind.backend.task.interfaces.rest.dto.UpdateTaskStatusRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/tasks")
@Validated
public class TaskController {

    private final TaskApplicationService taskApplicationService;

    public TaskController(TaskApplicationService taskApplicationService) {
        this.taskApplicationService = taskApplicationService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            AuthenticatedUser requester, @Valid @RequestBody CreateTaskRequest request) {
        try {
            Task created =
                    taskApplicationService.create(
                            requester,
                            new CreateTaskCommand(
                                    request.userId(),
                                    request.projectId(),
                                    request.assigneeId(),
                                    request.parentTaskId(),
                                    request.taskLevel(),
                                    request.taskType(),
                                    request.storyPoints(),
                                    request.releaseVersion(),
                                    request.title(),
                                    request.description(),
                                    request.status(),
                                    request.priority(),
                                    request.dueAt(),
                                    request.durationMinutes(),
                                    request.energyLevel(),
                                    request.source(),
                                    request.confidence()));
            return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(created));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }

    @GetMapping
    public List<TaskResponse> listTasks(
            AuthenticatedUser requester,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(defaultValue = "false") boolean overdueOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {
        return taskApplicationService
                .list(
                        requester,
                        Optional.ofNullable(userId),
                        Optional.ofNullable(status),
                        overdueOnly,
                        page,
                        size)
                .stream()
                .map(TaskResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(AuthenticatedUser requester, @PathVariable UUID id) {
        return taskApplicationService
                .findById(requester, id)
                .map(TaskResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/children")
    public List<TaskResponse> children(AuthenticatedUser requester, @PathVariable UUID id) {
        return taskApplicationService.children(requester, id).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @GetMapping("/{id}/ancestors")
    public List<TaskResponse> ancestors(AuthenticatedUser requester, @PathVariable UUID id) {
        return taskApplicationService.ancestors(requester, id).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @GetMapping("/{id}/completion")
    public ResponseEntity<TaskCompletionResponse> getCompletion(
            AuthenticatedUser requester, @PathVariable UUID id) {
        return taskApplicationService
                .findById(requester, id)
                .map(
                        task ->
                                ResponseEntity.ok(
                                        new TaskCompletionResponse(
                                                task.id(),
                                                task.status(),
                                                task.status() == TaskStatus.DONE,
                                                task.updatedAt())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            AuthenticatedUser requester,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskRequest request) {
        try {
            return taskApplicationService
                    .update(
                            requester,
                            id,
                            new UpdateTaskCommand(
                                    request.version(),
                                    request.projectId(),
                                    request.assigneeId(),
                                    request.parentTaskId(),
                                    request.taskLevel(),
                                    request.taskType(),
                                    request.storyPoints(),
                                    request.releaseVersion(),
                                    request.title(),
                                    request.description(),
                                    request.status(),
                                    request.priority(),
                                    request.dueAt(),
                                    request.durationMinutes(),
                                    request.energyLevel()))
                    .map(TaskResponse::from)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            AuthenticatedUser requester,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        try {
            return taskApplicationService
                    .updateStatus(requester, id, request.status())
                    .map(TaskResponse::from)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<TaskResponse> archiveTask(AuthenticatedUser requester, @PathVariable UUID id) {
        try {
            return taskApplicationService
                    .archive(requester, id)
                    .map(TaskResponse::from)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }
}
