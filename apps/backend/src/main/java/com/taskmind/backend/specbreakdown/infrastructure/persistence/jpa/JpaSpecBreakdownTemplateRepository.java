package com.taskmind.backend.specbreakdown.infrastructure.persistence.jpa;

import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownTemplate;
import com.taskmind.backend.specbreakdown.domain.repository.SpecBreakdownTemplateRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaSpecBreakdownTemplateRepository implements SpecBreakdownTemplateRepository {
    private final SpringDataSpecBreakdownTemplateRepository repo;

    JpaSpecBreakdownTemplateRepository(SpringDataSpecBreakdownTemplateRepository repo) {
        this.repo = repo;
    }

    public SpecBreakdownTemplate save(SpecBreakdownTemplate template) {
        return repo.saveAndFlush(SpecBreakdownTemplateJpaEntity.from(template)).toDomain();
    }

    public Optional<SpecBreakdownTemplate> findById(UUID id) {
        return repo.findById(id).map(SpecBreakdownTemplateJpaEntity::toDomain);
    }

    public List<SpecBreakdownTemplate> findByProjectId(UUID projectId) {
        return repo.findByProjectIdOrderByUpdatedAtDesc(projectId).stream()
                .map(SpecBreakdownTemplateJpaEntity::toDomain)
                .toList();
    }

    public void delete(SpecBreakdownTemplate template) {
        repo.delete(SpecBreakdownTemplateJpaEntity.from(template));
        repo.flush();
    }
}
