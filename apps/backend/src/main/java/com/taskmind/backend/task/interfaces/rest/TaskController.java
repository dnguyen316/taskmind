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
import com.taskmind.backend.security.AuthenticatedUserResolver;
import org.springframework.security.core.Authentication;
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
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public TaskController(TaskApplicationService taskApplicationService, AuthenticatedUserResolver authenticatedUserResolver) { this.taskApplicationService = taskApplicationService; this.authenticatedUserResolver = authenticatedUserResolver; }

    @PostMapping
    public ResponseEntity<Task> createTask(
        @RequestHeader(value = "X-User-Id", required = false) UUID requesterUserId,
        Authentication authentication,
        @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
        @Valid @RequestBody CreateTaskRequest request
    ) {
        try {
            var requester = authenticatedUserResolver.resolve(authentication, requesterUserId, rolesHeader);
            var created = taskApplicationService.create(requester, new CreateTaskCommand(
            request.userId(),
            request.projectId(), request.assigneeId(), request.parentTaskId(), request.taskLevel(), request.taskType(), request.storyPoints(), request.releaseVersion(),
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
        @RequestHeader(value = "X-User-Id", required = false) UUID requesterUserId,
        Authentication authentication,
        @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(defaultValue = "false") boolean overdueOnly,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        return taskApplicationService.list(
            authenticatedUserResolver.resolve(authentication, requesterUserId, rolesHeader),
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
        @RequestHeader(value = "X-User-Id", required = false) UUID requesterUserId,
        Authentication authentication,
        @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateTaskRequest request
    ) {
        try {
            return taskApplicationService.update(authenticatedUserResolver.resolve(authentication, requesterUserId, rolesHeader), id, new UpdateTaskCommand(
                request.version(), request.projectId(), request.assigneeId(), request.parentTaskId(), request.taskLevel(), request.taskType(), request.storyPoints(), request.releaseVersion(),
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
        @RequestHeader(value = "X-User-Id", required = false) UUID requesterUserId,
        Authentication authentication,
        @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        return taskApplicationService.updateStatus(authenticatedUserResolver.resolve(authentication, requesterUserId, rolesHeader), id, request.status())
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Task> archiveTask(
        @RequestHeader(value = "X-User-Id", required = false) UUID requesterUserId,
        Authentication authentication,
        @RequestHeader(value = "X-User-Roles", required = false) String rolesHeader,
        @PathVariable UUID id
    ) {
        return taskApplicationService.archive(authenticatedUserResolver.resolve(authentication, requesterUserId, rolesHeader), id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @GetMapping("/{id}")
    public ResponseEntity<Task> getTask(@PathVariable UUID id, @RequestHeader(value="X-User-Id",required=false) UUID userId, @RequestHeader(value="X-User-Roles",required=false) String roles, Authentication authentication) {
        return taskApplicationService.findById(authenticatedUserResolver.resolve(authentication,userId,roles),id).map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());
    }
    @GetMapping("/{id}/children")
    public List<Task> children(@PathVariable UUID id,@RequestHeader(value="X-User-Id",required=false) UUID userId,@RequestHeader(value="X-User-Roles",required=false) String roles,Authentication authentication){return taskApplicationService.children(authenticatedUserResolver.resolve(authentication,userId,roles),id);}
    @GetMapping("/{id}/ancestors")
    public List<Task> ancestors(@PathVariable UUID id,@RequestHeader(value="X-User-Id",required=false) UUID userId,@RequestHeader(value="X-User-Roles",required=false) String roles,Authentication authentication){return taskApplicationService.ancestors(authenticatedUserResolver.resolve(authentication,userId,roles),id);}
}
