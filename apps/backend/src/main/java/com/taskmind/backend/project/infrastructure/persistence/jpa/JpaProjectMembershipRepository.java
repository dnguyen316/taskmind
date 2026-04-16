package com.taskmind.backend.project.infrastructure.persistence.jpa;

import com.taskmind.backend.project.domain.model.ProjectMembership;
import com.taskmind.backend.project.domain.repository.ProjectMembershipRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProjectMembershipRepository implements ProjectMembershipRepository {

    private final SpringDataProjectMembershipJpaRepository projectMembershipJpaRepository;

    public JpaProjectMembershipRepository(SpringDataProjectMembershipJpaRepository projectMembershipJpaRepository) {
        this.projectMembershipJpaRepository = projectMembershipJpaRepository;
    }

    @Override
    public ProjectMembership save(ProjectMembership membership) {
        var persisted = projectMembershipJpaRepository.save(ProjectMembershipJpaEntity.fromDomain(membership));
        return persisted.toDomain();
    }

    @Override
    public void deleteByProjectIdAndUserId(UUID projectId, UUID userId) {
        projectMembershipJpaRepository.deleteByProjectIdAndUserId(projectId, userId);
    }

    @Override
    public List<ProjectMembership> findByProjectId(UUID projectId) {
        return projectMembershipJpaRepository.findByProjectId(projectId)
            .stream()
            .map(ProjectMembershipJpaEntity::toDomain)
            .toList();
    }

    @Override
    public boolean existsByProjectIdAndUserId(UUID projectId, UUID userId) {
        return projectMembershipJpaRepository.existsByProjectIdAndUserId(projectId, userId);
    }
}
