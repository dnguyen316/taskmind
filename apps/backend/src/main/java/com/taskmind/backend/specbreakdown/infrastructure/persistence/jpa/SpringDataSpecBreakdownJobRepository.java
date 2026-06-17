package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;
import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
interface SpringDataSpecBreakdownJobRepository extends JpaRepository<SpecBreakdownJobJpaEntity, UUID>{ List<SpecBreakdownJobJpaEntity> findByDraftIdOrderByCreatedAtDesc(UUID draftId); }
