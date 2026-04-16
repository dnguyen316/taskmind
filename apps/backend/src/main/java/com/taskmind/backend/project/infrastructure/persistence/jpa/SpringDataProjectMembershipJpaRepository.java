package com.taskmind.backend.project.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataProjectMembershipJpaRepository
    extends JpaRepository<ProjectMembershipJpaEntity, ProjectMembershipJpaId> {

    List<ProjectMembershipJpaEntity> findByProjectId(UUID projectId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    void deleteByProjectIdAndUserId(UUID projectId, UUID userId);
}
