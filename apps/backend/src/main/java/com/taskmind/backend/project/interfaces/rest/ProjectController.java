package com.taskmind.backend.project.interfaces.rest;

import com.taskmind.backend.project.application.ArchiveProjectCommand;
import com.taskmind.backend.project.application.CreateProjectCommand;
import com.taskmind.backend.project.application.ProjectApplicationService;
import com.taskmind.backend.project.application.UpdateProjectCommand;
import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.backend.project.interfaces.rest.dto.CreateProjectRequest;
import com.taskmind.backend.project.interfaces.rest.dto.UpdateProjectRequest;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/v1/projects")
@Validated
public class ProjectController {

    private final ProjectApplicationService projectApplicationService;

    public ProjectController(ProjectApplicationService projectApplicationService) {
        this.projectApplicationService = projectApplicationService;
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@Valid @RequestBody CreateProjectRequest request) {
        try {
            var created = projectApplicationService.create(new CreateProjectCommand(
                request.name(),
                request.key(),
                request.description(),
                request.ownerUserId()
            ));
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    @GetMapping
    public List<Project> listProjects(@RequestParam(defaultValue = "false") boolean includeArchived) {
        return projectApplicationService.list(includeArchived);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(@PathVariable UUID id) {
        return projectApplicationService.findById(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Project> updateProject(@PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest request) {
        try {
            return projectApplicationService.update(id, new UpdateProjectCommand(
                    request.name(),
                    request.key(),
                    request.description()
                ))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Project> archiveProject(@PathVariable UUID id) {
        return projectApplicationService.archive(new ArchiveProjectCommand(id))
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
