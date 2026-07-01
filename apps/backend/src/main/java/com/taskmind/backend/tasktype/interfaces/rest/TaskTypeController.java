package com.taskmind.backend.tasktype.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.tasktype.application.TaskTypeApplicationService;
import com.taskmind.backend.tasktype.application.TaskTypeForbiddenException;
import com.taskmind.backend.tasktype.interfaces.rest.dto.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/task-types")
public class TaskTypeController {
    private final TaskTypeApplicationService service;

    public TaskTypeController(TaskTypeApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<TaskTypeDefinitionResponse> list(
            AuthenticatedUser requester, @RequestParam(required = false) UUID projectId) {
        return service.listActive(requester, projectId).stream()
                .map(TaskTypeDefinitionResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<TaskTypeDefinitionResponse> create(
            AuthenticatedUser requester, @Valid @RequestBody CreateTaskTypeRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(TaskTypeDefinitionResponse.from(
                            service.create(
                                    requester,
                                    request.projectId(),
                                    request.key(),
                                    request.name(),
                                    request.color(),
                                    request.icon(),
                                    request.sortOrder())));
        } catch (TaskTypeForbiddenException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TaskTypeDefinitionResponse> update(
            AuthenticatedUser requester, @PathVariable UUID id, @RequestBody UpdateTaskTypeRequest request) {
        try {
            return service.update(
                            requester,
                            id,
                            request.name(),
                            request.color(),
                            request.icon(),
                            request.active(),
                            request.sortOrder())
                    .map(TaskTypeDefinitionResponse::from)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (TaskTypeForbiddenException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e);
        }
    }
}
