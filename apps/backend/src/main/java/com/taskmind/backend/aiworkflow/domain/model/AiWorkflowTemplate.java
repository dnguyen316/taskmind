package com.taskmind.backend.aiworkflow.domain.model;

import java.time.Instant;
import java.util.UUID;

public record AiWorkflowTemplate(
        UUID id,
        Long version,
        UUID projectId,
        String name,
        String description,
        WorkflowType workflowType,
        String templateBody,
        String allowedTools,
        ApprovalPolicy approvalPolicy,
        String defaultModelPolicy,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt) {
    public boolean archived() {
        return archivedAt != null;
    }
}
