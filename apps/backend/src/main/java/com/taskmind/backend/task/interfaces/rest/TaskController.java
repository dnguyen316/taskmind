package com.taskmind.backend.task.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.task.application.CreateTaskCommand;
import com.taskmind.backend.task.application.TaskApplicationService;
import com.taskmind.backend.task.application.UpdateTaskCommand;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.interfaces.rest.dto.CreateTaskRequest;
import com.taskmind.backend.task.interfaces.rest.dto.TaskCompletionResponse;
import com.taskmind.backend.task.interfaces.rest.dto.UpdateTaskRequest;
import com.taskmind.backend.task.interfaces.rest.dto.UpdateTaskStatusRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.springframework.web.bind.annotation.RequestHeader;
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
    public ResponseEntity<Task> createTask(
        @RequestHeader("X-User-Id") UUID requesterUserId,
        @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
        @Valid @RequestBody CreateTaskRequest request
    ) {
        try {
            var requester = toAuthenticatedUser(requesterUserId, rolesHeader);
            var created = taskApplicationService.create(requester, new CreateTaskCommand(
            request.userId(),
            request.projectId(),
            request.title(),
            request.description(),
            request.status(),
            request.priority(),
            request.dueAt(),
            request.durationMinutes(),
            request.energyLevel(),
            request.source(),
            request.confidence()
        ));
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }

    @GetMapping
    public List<Task> listTasks(
        @RequestHeader("X-User-Id") UUID requesterUserId,
        @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(defaultValue = "false") boolean overdueOnly,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        return taskApplicationService.list(
            toAuthenticatedUser(requesterUserId, rolesHeader),
            java.util.Optional.ofNullable(userId),
            java.util.Optional.ofNullable(status),
            overdueOnly,
            page,
            size
        );
    }

    @GetMapping("/{id}/completion")
    public ResponseEntity<TaskCompletionResponse> getCompletion(@PathVariable UUID id) {
        return taskApplicationService.findById(id)
            .map(task -> ResponseEntity.ok(new TaskCompletionResponse(
                task.id(),
                task.status(),
                task.status() == TaskStatus.DONE,
                task.updatedAt()
            )))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Task> updateTask(
        @RequestHeader("X-User-Id") UUID requesterUserId,
        @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateTaskRequest request
    ) {
        try {
            return taskApplicationService.update(toAuthenticatedUser(requesterUserId, rolesHeader), id, new UpdateTaskCommand(
                request.projectId(),
                request.title(),
                request.description(),
                request.status(),
                request.priority(),
                request.dueAt(),
                request.durationMinutes(),
                request.energyLevel()
            ))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateTaskStatus(
        @RequestHeader("X-User-Id") UUID requesterUserId,
        @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return taskApplicationService.updateStatus(toAuthenticatedUser(requesterUserId, rolesHeader), id, request.status())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Task> archiveTask(
        @RequestHeader("X-User-Id") UUID requesterUserId,
        @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
        @PathVariable UUID id
    ) {
        return taskApplicationService.archive(toAuthenticatedUser(requesterUserId, rolesHeader), id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private AuthenticatedUser toAuthenticatedUser(UUID userId, String rolesHeader) {
        var roles = rolesHeader == null || rolesHeader.isBlank()
            ? Set.<String>of()
            : java.util.Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
        return new AuthenticatedUser(userId, roles);
    }
}
