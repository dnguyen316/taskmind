package com.taskmind.backend.project.interfaces.rest;

import com.taskmind.backend.project.application.ProjectMembershipApplicationService;
import com.taskmind.backend.project.domain.model.ProjectMembership;
import com.taskmind.backend.project.interfaces.rest.dto.AddProjectMemberRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/projects/{projectId}/members")
@Validated
public class ProjectMembershipController {

    private final ProjectMembershipApplicationService projectMembershipApplicationService;

    public ProjectMembershipController(ProjectMembershipApplicationService projectMembershipApplicationService) {
        this.projectMembershipApplicationService = projectMembershipApplicationService;
    }

    @PostMapping
    public ResponseEntity<ProjectMembership> addMember(
        @PathVariable UUID projectId,
        @Valid @RequestBody AddProjectMemberRequest request
    ) {
        try {
            var membership = projectMembershipApplicationService.addMember(projectId, request.userId(), request.role());
            return ResponseEntity.status(HttpStatus.CREATED).body(membership);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage(), e);
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID projectId, @PathVariable UUID userId) {
        projectMembershipApplicationService.removeMember(projectId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ProjectMembership> listMembers(@PathVariable UUID projectId) {
        return projectMembershipApplicationService.listMembers(projectId);
    }
}
