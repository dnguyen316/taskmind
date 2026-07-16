package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;

import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownDraft;
import com.taskmind.backend.specbreakdown.domain.repository.SpecBreakdownDraftRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaSpecBreakdownDraftRepository implements SpecBreakdownDraftRepository {
    private final SpringDataSpecBreakdownDraftRepository draftRepository;

    JpaSpecBreakdownDraftRepository(SpringDataSpecBreakdownDraftRepository draftRepository) {
        this.draftRepository = draftRepository;
    }

    public SpecBreakdownDraft save(SpecBreakdownDraft draft) {
        return draftRepository.saveAndFlush(SpecBreakdownDraftJpaEntity.from(draft)).toDomain();
    }

    public Optional<SpecBreakdownDraft> findById(UUID id) {
        return draftRepository.findById(id).map(SpecBreakdownDraftJpaEntity::toDomain);
    }

    public List<SpecBreakdownDraft> findByProjectId(UUID projectId) {
        return draftRepository.findByProjectIdOrderByUpdatedAtDesc(projectId).stream()
                .map(SpecBreakdownDraftJpaEntity::toDomain)
                .toList();
    }
}
