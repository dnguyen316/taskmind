package com.taskmind.backend.aiworkflow.domain.repository;

import com.taskmind.backend.aiworkflow.domain.model.AiWorkflowTemplate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiWorkflowTemplateRepository {
    AiWorkflowTemplate save(AiWorkflowTemplate template);

    Optional<AiWorkflowTemplate> findById(UUID id);

    List<AiWorkflowTemplate> findActiveByProjectId(UUID projectId);
}
