package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;

import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownAttachment;
import com.taskmind.backend.specbreakdown.domain.repository.SpecBreakdownAttachmentRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaSpecBreakdownAttachmentRepository implements SpecBreakdownAttachmentRepository {
    private final SpringDataSpecBreakdownAttachmentRepository repo;

    JpaSpecBreakdownAttachmentRepository(SpringDataSpecBreakdownAttachmentRepository repo) {
        this.repo = repo;
    }

    public SpecBreakdownAttachment save(SpecBreakdownAttachment attachment) {
        return repo.saveAndFlush(SpecBreakdownAttachmentJpaEntity.from(attachment)).toDomain();
    }

    public Optional<SpecBreakdownAttachment> findActiveById(UUID id) {
        return repo.findActiveById(id).map(SpecBreakdownAttachmentJpaEntity::toDomain);
    }

    public List<SpecBreakdownAttachment> findActiveByDraftId(UUID draftId) {
        return repo.findActiveByDraftId(draftId).stream().map(SpecBreakdownAttachmentJpaEntity::toDomain).toList();
    }
}
