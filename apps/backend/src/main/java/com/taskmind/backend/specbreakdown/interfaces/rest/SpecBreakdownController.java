package com.taskmind.backend.specbreakdown.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.specbreakdown.application.SpecBreakdownApplicationService;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownJobType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/spec-breakdown")
public class SpecBreakdownController {
    private final SpecBreakdownApplicationService service;

    public SpecBreakdownController(SpecBreakdownApplicationService service) {
        this.service = service;
    }

    @PostMapping("/drafts")
    public ResponseEntity<?> create(
            AuthenticatedUser user, @Valid @RequestBody CreateDraftRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createDraft(user, request.toCommand()));
    }

    @GetMapping("/drafts/{id}")
    public Object get(AuthenticatedUser user, @PathVariable UUID id) {
        return service.getDraft(user, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/drafts/{id}/jobs")
    public ResponseEntity<?> start(
            AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody StartJobRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(service.startJob(user, id, request.aiJobType()));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @GetMapping("/jobs/{id}")
    public Object job(AuthenticatedUser user, @PathVariable UUID id) {
        return service.getJob(user, id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/jobs/{id}/{command:pause|resume|cancel}")
    public Object command(
            AuthenticatedUser user, @PathVariable UUID id, @PathVariable String command) {
        return service.commandJob(user, id, command);
    }

    @PostMapping("/drafts/{id}/review")
    public Object review(
            AuthenticatedUser user, @PathVariable UUID id, @RequestBody ReviewRequest request) {
        return service.review(user, id, request.toCommand());
    }

    @PostMapping("/drafts/{id}/materialize")
    public Map<String, Object> materialize(AuthenticatedUser user, @PathVariable UUID id) {
        return Map.of("taskIds", service.materialize(user, id));
    }

    public record CreateDraftRequest(
            @NotNull UUID projectId,
            UUID templateId,
            @NotBlank String title,
            @NotBlank String rawSpec,
            String richContent,
            String candidateTree,
            String fixVersion,
            String affectedVersion,
            String sprint,
            String issueType,
            String publishKey) {
        SpecBreakdownApplicationService.CreateDraftCommand toCommand() {
            return new SpecBreakdownApplicationService.CreateDraftCommand(
                    projectId,
                    templateId,
                    title,
                    rawSpec,
                    richContent,
                    candidateTree,
                    fixVersion,
                    affectedVersion,
                    sprint,
                    issueType,
                    publishKey);
        }
    }

    public record StartJobRequest(@NotNull SpecBreakdownJobType aiJobType) {}

    public record ReviewRequest(boolean accepted, String candidateTree) {
        SpecBreakdownApplicationService.ReviewCommand toCommand() {
            return new SpecBreakdownApplicationService.ReviewCommand(accepted, candidateTree);
        }
    }
}
