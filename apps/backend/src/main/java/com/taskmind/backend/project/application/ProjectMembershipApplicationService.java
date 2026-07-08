package com.taskmind.backend.project.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.project.domain.model.ProjectMembership;
import com.taskmind.backend.project.domain.model.ProjectMembershipRole;
import com.taskmind.backend.project.domain.repository.ProjectMembershipRepository;
import com.taskmind.backend.project.domain.repository.ProjectRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ProjectMembershipApplicationService {

    private final ProjectMembershipRepository projectMembershipRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    public ProjectMembershipApplicationService(
            ProjectMembershipRepository projectMembershipRepository,
            ProjectRepository projectRepository,
            ProjectAuthorizationService projectAuthorizationService) {
        this.projectMembershipRepository = projectMembershipRepository;
        this.projectRepository = projectRepository;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    public ProjectMembership addMember(
            AuthenticatedUser actor, UUID projectId, UUID userId, ProjectMembershipRole role) {
        assertProjectExists(projectId);
        assertCanManageMembers(actor, projectId);
        try {
            return projectMembershipRepository.save(new ProjectMembership(projectId, userId, role));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Membership already exists", e);
        }
    }

    public ProjectMembership changeMemberRole(
            AuthenticatedUser actor, UUID projectId, UUID userId, ProjectMembershipRole role) {
        assertProjectExists(projectId);
        assertCanManageMembers(actor, projectId);
        projectMembershipRepository
                .findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ProjectMembershipNotFoundException("Membership not found"));
        return projectMembershipRepository.save(new ProjectMembership(projectId, userId, role));
    }

    public void removeMember(AuthenticatedUser actor, UUID projectId, UUID userId) {
        assertProjectExists(projectId);
        assertCanManageMembers(actor, projectId);
        projectMembershipRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    public List<ProjectMembership> listMembers(AuthenticatedUser actor, UUID projectId) {
        assertProjectExists(projectId);
        assertCanListMembers(actor, projectId);
        return projectMembershipRepository.findByProjectId(projectId).stream()
                .sorted(Comparator.comparing(ProjectMembership::userId))
                .toList();
    }

    public boolean isMember(UUID projectId, UUID userId) {
        return projectMembershipRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public void validateMembership(UUID projectId, UUID userId) {
        if (projectId != null && !isMember(projectId, userId) && !isProjectOwner(userId, projectId)) {
            throw new IllegalArgumentException("User is not a member of the project");
        }
    }

    private boolean isProjectOwner(UUID userId, UUID projectId) {
        return projectRepository.findById(projectId)
                .map(project -> project.ownerUserId().equals(userId))
                .orElse(false);
    }

    private void assertProjectExists(UUID projectId) {
        if (projectRepository.findById(projectId).isEmpty()) {
            throw new ProjectMembershipNotFoundException("Project not found");
        }
    }

    private void assertCanManageMembers(AuthenticatedUser actor, UUID projectId) {
        if (!projectAuthorizationService.canManageMembers(actor, projectId)) {
            throw new ProjectMembershipForbiddenException("Actor is not allowed to manage project members");
        }
    }

    private void assertCanListMembers(AuthenticatedUser actor, UUID projectId) {
        if (!projectAuthorizationService.canListMembers(actor, projectId)) {
            throw new ProjectMembershipForbiddenException("Actor is not allowed to list project members");
        }
    }
}
