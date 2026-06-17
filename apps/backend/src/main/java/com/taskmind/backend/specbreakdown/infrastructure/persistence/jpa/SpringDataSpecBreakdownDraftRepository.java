package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
interface SpringDataSpecBreakdownDraftRepository extends JpaRepository<SpecBreakdownDraftJpaEntity, UUID>{ List<SpecBreakdownDraftJpaEntity> findByProjectIdOrderByUpdatedAtDesc(UUID projectId); }
