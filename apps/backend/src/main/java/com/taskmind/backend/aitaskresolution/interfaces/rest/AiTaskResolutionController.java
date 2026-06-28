package com.taskmind.backend.aitaskresolution.interfaces.rest;

import com.taskmind.backend.aitaskresolution.application.*;
import com.taskmind.backend.aitaskresolution.interfaces.rest.dto.*;
import com.taskmind.backend.auth.AuthenticatedUser;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AiTaskResolutionController {
    private final AiTaskResolutionApplicationService service;
    public AiTaskResolutionController(AiTaskResolutionApplicationService service) { this.service = service; }

    @PostMapping("/v1/tasks/{taskId}/ai-resolution-jobs")
    public ResponseEntity<AiTaskResolutionJobResponse> create(AuthenticatedUser user, @PathVariable UUID taskId, @RequestBody(required = false) AiTaskResolutionJobRequest request) {
        AiTaskResolutionJobRequest r = request == null ? new AiTaskResolutionJobRequest(null, null, null) : request;
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(AiTaskResolutionJobResponse.from(service.create(user, taskId, new CreateAiTaskResolutionJobCommand(r.templateId(), r.githubProjectLinkId(), r.idempotencyKey()))));
        } catch (NoSuchElementException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e); }
    }

    @GetMapping("/v1/tasks/{taskId}/ai-resolution-jobs")
    public List<AiTaskResolutionJobResponse> list(AuthenticatedUser user, @PathVariable UUID taskId) {
        try { return service.list(user, taskId).stream().map(AiTaskResolutionJobResponse::from).toList(); }
        catch (NoSuchElementException e) { throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e); }
    }

    @GetMapping("/v1/ai-resolution-jobs/{jobId}")
    public ResponseEntity<AiTaskResolutionJobResponse> get(AuthenticatedUser user, @PathVariable UUID jobId) { return service.get(user, jobId).map(AiTaskResolutionJobResponse::from).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build()); }

    @PostMapping("/v1/ai-resolution-jobs/{jobId}/cancel")
    public ResponseEntity<AiTaskResolutionJobResponse> cancel(AuthenticatedUser user, @PathVariable UUID jobId) { return service.cancel(user, jobId).map(AiTaskResolutionJobResponse::from).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build()); }

    @PostMapping("/v1/ai-resolution-jobs/{jobId}/approve")
    public ResponseEntity<AiTaskResolutionJobResponse> approve(AuthenticatedUser user, @PathVariable UUID jobId) { return service.approve(user, jobId).map(AiTaskResolutionJobResponse::from).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build()); }
}
