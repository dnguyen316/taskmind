package com.taskmind.backend.specbreakdown.template;

import com.taskmind.backend.specbreakdown.application.SpecBreakdownTemplateApplicationService;
import com.taskmind.backend.specbreakdown.application.SpecBreakdownTemplateApplicationService.TemplateCommand;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownTemplate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpecBreakdownTemplateController {
    private final SpecBreakdownTemplateApplicationService service;

    public SpecBreakdownTemplateController(SpecBreakdownTemplateApplicationService service) {
        this.service = service;
    }

    @PostMapping("/v1/projects/{projectId}/spec-templates")
    public ResponseEntity<SpecBreakdownTemplate> create(
            @PathVariable UUID projectId, @Valid @RequestBody TemplateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(projectId, request.toCommand()));
    }

    @GetMapping("/v1/projects/{projectId}/spec-templates")
    public List<SpecBreakdownTemplate> list(@PathVariable UUID projectId) {
        return service.listByProject(projectId);
    }

    @PutMapping("/v1/spec-templates/{id}")
    public ResponseEntity<SpecBreakdownTemplate> update(
            @PathVariable UUID id, @Valid @RequestBody TemplateRequest request) {
        return service.update(id, request.toCommand())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/v1/spec-templates/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return service.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    public record TemplateRequest(@NotBlank String name, String description, String fields) {
        TemplateCommand toCommand() {
            return new TemplateCommand(name, description, fields);
        }
    }
}
