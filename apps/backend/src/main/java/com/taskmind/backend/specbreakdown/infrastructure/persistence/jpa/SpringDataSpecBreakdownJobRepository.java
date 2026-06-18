package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;

import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownJobStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

interface SpringDataSpecBreakdownJobRepository extends JpaRepository<SpecBreakdownJobJpaEntity, UUID> {
    List<SpecBreakdownJobJpaEntity> findByDraftIdOrderByCreatedAtDesc(UUID draftId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SpecBreakdownJobJpaEntity> findFirstByStatusOrderByCreatedAtAsc(SpecBreakdownJobStatus status);
}
