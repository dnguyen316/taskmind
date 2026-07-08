package com.taskmind.backend.project.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.project.application.ArchiveProjectCommand;
import com.taskmind.backend.project.application.CreateProjectCommand;
import com.taskmind.backend.project.application.ProjectApplicationService;
import com.taskmind.backend.project.application.ProjectMembershipApplicationService;
import com.taskmind.backend.project.application.UpdateProjectCommand;
import com.taskmind.backend.project.application.health.ProjectHealthApplicationService;
import com.taskmind.backend.project.application.health.ProjectHealthResponse;
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
    private final ProjectMembershipApplicationService projectMembershipApplicationService;
    private final ProjectHealthApplicationService projectHealthApplicationService;

    public ProjectController(
            ProjectApplicationService projectApplicationService,
            ProjectMembershipApplicationService projectMembershipApplicationService,
            ProjectHealthApplicationService projectHealthApplicationService) {
        this.projectApplicationService = projectApplicationService;
        this.projectMembershipApplicationService = projectMembershipApplicationService;
        this.projectHealthApplicationService = projectHealthApplicationService;
    }

    @PostMapping
    public ResponseEntity<Project> createProject(
            AuthenticatedUser requester, @Valid @RequestBody CreateProjectRequest request) {
        try {
            UUID ownerUserId = requester.userId();
            if (requester.isPrivileged() && request.ownerUserId() != null) {
                ownerUserId = request.ownerUserId();
            }
            Project created =
                    projectApplicationService.create(
                            new CreateProjectCommand(
                                    request.name(),
                                    request.key(),
                                    request.description(),
                                    ownerUserId));
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    @GetMapping
    public List<Project> listProjects(
            AuthenticatedUser requester,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return projectApplicationService.list(includeArchived).stream()
                .filter(project -> canRead(requester, project))
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProject(AuthenticatedUser requester, @PathVariable UUID id) {
        return projectApplicationService
                .findById(id)
                .filter(project -> canRead(requester, project))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/health")
    public ResponseEntity<ProjectHealthResponse> getProjectHealth(
            AuthenticatedUser requester, @PathVariable UUID id) {
        return projectApplicationService
                .findById(id)
                .filter(project -> canRead(requester, project))
                .map(project -> ResponseEntity.ok(projectHealthApplicationService.calculate(project.id())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            AuthenticatedUser requester,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        authorizeOwner(requester, id);
        try {
            return projectApplicationService
                    .update(
                            id,
                            new UpdateProjectCommand(
                                    request.name(), request.key(), request.description()))
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Project> archiveProject(
            AuthenticatedUser requester, @PathVariable UUID id) {
        authorizeOwner(requester, id);
        return projectApplicationService
                .archive(new ArchiveProjectCommand(id))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean canRead(AuthenticatedUser requester, Project project) {
        return requester.isPrivileged()
                || project.ownerUserId().equals(requester.userId())
                || projectMembershipApplicationService.isMember(project.id(), requester.userId());
    }

    private void authorizeOwner(AuthenticatedUser requester, UUID projectId) {
        Project project = projectApplicationService.findById(projectId).orElseThrow();
        if (!requester.isPrivileged() && !project.ownerUserId().equals(requester.userId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Only the project owner can mutate the project");
        }
    }
}
