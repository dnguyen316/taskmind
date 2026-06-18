package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSpecBreakdownTemplateRepository extends JpaRepository<SpecBreakdownTemplateJpaEntity, UUID> {
    List<SpecBreakdownTemplateJpaEntity> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);
}
