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
        boolean autoApproveReadOnly,
        boolean requireApprovalForComments,
        boolean requireApprovalForBranch,
        boolean requireApprovalForPullRequest,
        boolean requireApprovalForTaskMutation,
        String defaultModelPolicy,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt) {
    public AiWorkflowTemplate(
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
        this(
                id,
                version,
                projectId,
                name,
                description,
                workflowType,
                templateBody,
                allowedTools,
                approvalPolicy,
                true,
                false,
                true,
                true,
                true,
                defaultModelPolicy,
                archivedAt,
                createdAt,
                updatedAt);
    }

    public boolean archived() {
        return archivedAt != null;
    }
}
