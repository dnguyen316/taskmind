package com.taskmind.backend.aiworkflow.infrastructure.persistence.jpa;

import com.taskmind.backend.aiworkflow.domain.model.AiWorkflowTemplate;
import com.taskmind.backend.aiworkflow.domain.repository.AiWorkflowTemplateRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaAiWorkflowTemplateRepository implements AiWorkflowTemplateRepository {
    private final SpringDataAiWorkflowTemplateRepository repo;

    JpaAiWorkflowTemplateRepository(SpringDataAiWorkflowTemplateRepository repo) {
        this.repo = repo;
    }

    public AiWorkflowTemplate save(AiWorkflowTemplate template) {
        return repo.saveAndFlush(AiWorkflowTemplateJpaEntity.from(template)).toDomain();
    }

    public Optional<AiWorkflowTemplate> findById(UUID id) {
        return repo.findById(id).map(AiWorkflowTemplateJpaEntity::toDomain);
    }

    public List<AiWorkflowTemplate> findActiveByProjectId(UUID projectId) {
        return repo.findByProjectIdAndArchivedAtIsNullOrderByUpdatedAtDesc(projectId).stream()
                .map(AiWorkflowTemplateJpaEntity::toDomain)
                .toList();
    }
}
