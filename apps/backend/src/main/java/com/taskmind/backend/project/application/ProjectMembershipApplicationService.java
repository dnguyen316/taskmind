package com.taskmind.backend.project.application;

import com.taskmind.backend.project.domain.model.ProjectMembership;
import com.taskmind.backend.project.domain.model.ProjectMembershipRole;
import com.taskmind.backend.project.domain.repository.ProjectMembershipRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ProjectMembershipApplicationService {

    private final ProjectMembershipRepository projectMembershipRepository;

    public ProjectMembershipApplicationService(ProjectMembershipRepository projectMembershipRepository) {
        this.projectMembershipRepository = projectMembershipRepository;
    }

    public ProjectMembership addMember(UUID projectId, UUID userId, ProjectMembershipRole role) {
        try {
            return projectMembershipRepository.save(new ProjectMembership(projectId, userId, role));
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Membership already exists", e);
        }
    }

    public void removeMember(UUID projectId, UUID userId) {
        projectMembershipRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    public List<ProjectMembership> listMembers(UUID projectId) {
        return projectMembershipRepository.findByProjectId(projectId).stream()
            .sorted(Comparator.comparing(ProjectMembership::userId))
            .toList();
    }

    public boolean isMember(UUID projectId, UUID userId) {
        return projectMembershipRepository.existsByProjectIdAndUserId(projectId, userId);
    }

    public void validateMembership(UUID projectId, UUID userId) {
        if (projectId != null && !isMember(projectId, userId)) {
            throw new IllegalArgumentException("User is not a member of the project");
        }
    }
}
