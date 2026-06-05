package com.taskmind.backend.project.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface SpringDataProjectMembershipJpaRepository
    extends JpaRepository<ProjectMembershipJpaEntity, ProjectMembershipJpaId> {

    List<ProjectMembershipJpaEntity> findByProjectId(UUID projectId);

    Optional<ProjectMembershipJpaEntity> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    @Modifying
    @Transactional
    void deleteByProjectIdAndUserId(UUID projectId, UUID userId);
}
