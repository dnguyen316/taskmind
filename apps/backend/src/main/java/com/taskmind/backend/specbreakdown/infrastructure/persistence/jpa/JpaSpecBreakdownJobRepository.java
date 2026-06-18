package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;

import com.taskmind.backend.specbreakdown.application.SpecBreakdownProcessingJob;
import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownJobStatus;
import com.taskmind.backend.specbreakdown.domain.repository.SpecBreakdownJobRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaSpecBreakdownJobRepository implements SpecBreakdownJobRepository {
    private final SpringDataSpecBreakdownJobRepository repo;

    JpaSpecBreakdownJobRepository(SpringDataSpecBreakdownJobRepository repo) {
        this.repo = repo;
    }

    public SpecBreakdownProcessingJob save(SpecBreakdownProcessingJob job) {
        return repo.saveAndFlush(SpecBreakdownJobJpaEntity.from(job)).toDomain();
    }

    public Optional<SpecBreakdownProcessingJob> findById(UUID id) {
        return repo.findById(id).map(SpecBreakdownJobJpaEntity::toDomain);
    }

    public List<SpecBreakdownProcessingJob> findByDraftId(UUID draftId) {
        return repo.findByDraftIdOrderByCreatedAtDesc(draftId).stream()
                .map(SpecBreakdownJobJpaEntity::toDomain)
                .toList();
    }

    public Optional<SpecBreakdownProcessingJob> findFirstByStatusOrderByCreatedAt(SpecBreakdownJobStatus status) {
        return repo.findFirstByStatusOrderByCreatedAtAsc(status).map(SpecBreakdownJobJpaEntity::toDomain);
    }
}
