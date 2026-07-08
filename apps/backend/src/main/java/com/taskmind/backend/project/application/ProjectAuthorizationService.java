package com.taskmind.backend.project.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.backend.project.domain.model.ProjectMembership;
import com.taskmind.backend.project.domain.model.ProjectMembershipRole;
import com.taskmind.backend.project.domain.repository.ProjectMembershipRepository;
import com.taskmind.backend.project.domain.repository.ProjectRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProjectAuthorizationService {

    private final ProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;

    public ProjectAuthorizationService(
            ProjectRepository projectRepository,
            ProjectMembershipRepository projectMembershipRepository) {
        this.projectRepository = projectRepository;
        this.projectMembershipRepository = projectMembershipRepository;
    }

    public boolean canReadProject(AuthenticatedUser actor, Project project) {
        return actor.isPrivileged()
                || project.ownerUserId().equals(actor.userId())
                || membershipRole(project.id(), actor.userId()).isPresent();
    }

    public boolean canUpdateProject(AuthenticatedUser actor, Project project) {
        return canMutateProjectSettings(actor, project);
    }

    public boolean canArchiveProject(AuthenticatedUser actor, Project project) {
        return canMutateProjectSettings(actor, project);
    }

    public boolean canListMembers(AuthenticatedUser actor, UUID projectId) {
        return projectRepository.findById(projectId)
                .map(project -> actor.isPrivileged()
                        || project.ownerUserId().equals(actor.userId())
                        || membershipRole(projectId, actor.userId()).isPresent())
                .orElse(false);
    }

    public boolean canManageMembers(AuthenticatedUser actor, UUID projectId) {
        return projectRepository.findById(projectId)
                .map(project -> actor.isPrivileged()
                        || project.ownerUserId().equals(actor.userId())
                        || membershipRole(projectId, actor.userId())
                                .filter(ProjectMembershipRole::canManageMembers)
                                .isPresent())
                .orElse(false);
    }

    private boolean canMutateProjectSettings(AuthenticatedUser actor, Project project) {
        return actor.isPrivileged()
                || project.ownerUserId().equals(actor.userId())
                || membershipRole(project.id(), actor.userId())
                        .filter(ProjectMembershipRole::canMutateProjectSettings)
                        .isPresent();
    }

    private Optional<ProjectMembershipRole> membershipRole(UUID projectId, UUID userId) {
        return projectMembershipRepository.findByProjectIdAndUserId(projectId, userId)
                .map(ProjectMembership::role);
    }
}
