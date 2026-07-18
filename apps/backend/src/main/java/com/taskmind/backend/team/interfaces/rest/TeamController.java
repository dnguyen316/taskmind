package com.taskmind.backend.team.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.team.application.*;
import com.taskmind.backend.project.application.ProjectMembershipForbiddenException;
import com.taskmind.backend.project.application.ProjectMembershipNotFoundException;
import com.taskmind.backend.team.interfaces.rest.dto.*;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/team")
public class TeamController {
    private final TeamApplicationService service;
    private final TeamMembershipApplicationService memberships;

    public TeamController(TeamApplicationService service, TeamMembershipApplicationService memberships) {
        this.service = service;
        this.memberships = memberships;
    }

    @GetMapping("/directory")
    public TeamDirectoryResponse directory(AuthenticatedUser requester) {
        try {
            return service.directory(requester);
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to perform this operation.", e);
        }
    }

    @PostMapping("/members/{userId}/projects/{projectId}")
    public ResponseEntity<ProjectMembershipResponse> assignProjectMember(
            @PathVariable UUID userId,
            @PathVariable UUID projectId,
            AuthenticatedUser actor,
            @Valid @RequestBody AssignProjectMemberRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ProjectMembershipResponse.from(memberships.assignProjectMember(
                            actor, new AssignProjectMemberCommand(userId, projectId, request.role()))));
        } catch (SecurityException | ProjectMembershipForbiddenException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to perform this operation.", e);
        } catch (ProjectMembershipNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The requested resource was not found.", e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The request conflicts with the current resource state.", e);
        }
    }

    @PatchMapping("/members/{userId}/projects/{projectId}/role")
    public ProjectMembershipResponse changeProjectMemberRole(
            @PathVariable UUID userId,
            @PathVariable UUID projectId,
            AuthenticatedUser actor,
            @Valid @RequestBody ChangeProjectMemberRoleRequest request) {
        try {
            return ProjectMembershipResponse.from(memberships.changeProjectMemberRole(
                    actor, new ChangeProjectMemberRoleCommand(userId, projectId, request.role())));
        } catch (SecurityException | ProjectMembershipForbiddenException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to perform this operation.", e);
        } catch (ProjectMembershipNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The requested resource was not found.", e);
        }
    }

    @DeleteMapping("/members/{userId}/projects/{projectId}")
    public ResponseEntity<Void> removeProjectMember(
            @PathVariable UUID userId, @PathVariable UUID projectId, AuthenticatedUser actor) {
        try {
            memberships.removeProjectMember(actor, new RemoveProjectMemberCommand(userId, projectId));
            return ResponseEntity.noContent().build();
        } catch (SecurityException | ProjectMembershipForbiddenException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to perform this operation.", e);
        } catch (ProjectMembershipNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The requested resource was not found.", e);
        }
    }

    @PatchMapping("/members/{userId}/roles")
    public GlobalRoleResponse changeGlobalRole(
            @PathVariable UUID userId,
            AuthenticatedUser actor,
            @Valid @RequestBody ChangeGlobalRoleRequest request) {
        try {
            String role = memberships.changeGlobalRole(actor, new ChangeGlobalRoleCommand(userId, request.role()));
            return new GlobalRoleResponse(userId, role);
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to perform this operation.", e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The requested resource was not found.", e);
        }
    }
}
