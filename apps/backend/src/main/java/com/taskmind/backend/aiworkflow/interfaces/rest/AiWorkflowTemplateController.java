package com.taskmind.backend.aiworkflow.interfaces.rest;

import com.taskmind.backend.aiworkflow.application.AiWorkflowTemplateApplicationService;
import com.taskmind.backend.aiworkflow.application.AiWorkflowTemplateForbiddenException;
import com.taskmind.backend.aiworkflow.domain.model.AiWorkflowTemplate;
import com.taskmind.backend.aiworkflow.interfaces.rest.dto.AiWorkflowTemplateRequest;
import com.taskmind.backend.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class AiWorkflowTemplateController {
    private final AiWorkflowTemplateApplicationService service;

    public AiWorkflowTemplateController(AiWorkflowTemplateApplicationService service) {
        this.service = service;
    }

    @GetMapping("/v1/projects/{projectId}/ai-workflow-templates")
    public List<AiWorkflowTemplate> list(AuthenticatedUser actor, @PathVariable UUID projectId) {
        return service.list(actor, projectId);
    }

    @PostMapping("/v1/projects/{projectId}/ai-workflow-templates")
    public ResponseEntity<AiWorkflowTemplate> create(
            AuthenticatedUser actor, @PathVariable UUID projectId, @Valid @RequestBody AiWorkflowTemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(actor, projectId, request.toCommand()));
    }

    @GetMapping("/v1/ai-workflow-templates/{templateId}")
    public ResponseEntity<AiWorkflowTemplate> get(AuthenticatedUser actor, @PathVariable UUID templateId) {
        return service.get(actor, templateId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/v1/ai-workflow-templates/{templateId}")
    public ResponseEntity<AiWorkflowTemplate> update(
            AuthenticatedUser actor,
            @PathVariable UUID templateId,
            @Valid @RequestBody AiWorkflowTemplateRequest request) {
        return service.update(actor, templateId, request.toCommand())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/v1/ai-workflow-templates/{templateId}")
    public ResponseEntity<Void> archive(AuthenticatedUser actor, @PathVariable UUID templateId) {
        return service.archive(actor, templateId) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @ExceptionHandler(AiWorkflowTemplateForbiddenException.class)
    ResponseEntity<Void> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<Void> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
}
