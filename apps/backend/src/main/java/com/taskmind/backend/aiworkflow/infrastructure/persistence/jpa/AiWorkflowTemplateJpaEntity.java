package com.taskmind.backend.aiworkflow.infrastructure.persistence.jpa;

import com.taskmind.backend.aiworkflow.domain.model.AiWorkflowTemplate;
import com.taskmind.backend.aiworkflow.domain.model.ApprovalPolicy;
import com.taskmind.backend.aiworkflow.domain.model.WorkflowType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_workflow_templates")
public class AiWorkflowTemplateJpaEntity {
    @Id UUID id;
    @Version Long version;
    UUID projectId;
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    WorkflowType workflowType;

    @Column(columnDefinition = "TEXT")
    String templateBody;

    @Column(columnDefinition = "TEXT")
    String allowedTools;

    @Enumerated(EnumType.STRING)
    ApprovalPolicy approvalPolicy;

    boolean autoApproveReadOnly;
    boolean requireApprovalForComments;
    boolean requireApprovalForBranch;
    boolean requireApprovalForPullRequest;
    boolean requireApprovalForTaskMutation;

    @Column(columnDefinition = "TEXT")
    String defaultModelPolicy;

    Instant archivedAt;
    Instant createdAt;
    Instant updatedAt;

    protected AiWorkflowTemplateJpaEntity() {}

    static AiWorkflowTemplateJpaEntity from(AiWorkflowTemplate template) {
        AiWorkflowTemplateJpaEntity entity = new AiWorkflowTemplateJpaEntity();
        entity.id = template.id();
        entity.version = template.version();
        entity.projectId = template.projectId();
        entity.name = template.name();
        entity.description = template.description();
        entity.workflowType = template.workflowType();
        entity.templateBody = template.templateBody();
        entity.allowedTools = template.allowedTools();
        entity.approvalPolicy = template.approvalPolicy();
        entity.autoApproveReadOnly = template.autoApproveReadOnly();
        entity.requireApprovalForComments = template.requireApprovalForComments();
        entity.requireApprovalForBranch = template.requireApprovalForBranch();
        entity.requireApprovalForPullRequest = template.requireApprovalForPullRequest();
        entity.requireApprovalForTaskMutation = template.requireApprovalForTaskMutation();
        entity.defaultModelPolicy = template.defaultModelPolicy();
        entity.archivedAt = template.archivedAt();
        entity.createdAt = template.createdAt();
        entity.updatedAt = template.updatedAt();
        return entity;
    }

    AiWorkflowTemplate toDomain() {
        return new AiWorkflowTemplate(
                id,
                version,
                projectId,
                name,
                description,
                workflowType,
                templateBody,
                allowedTools,
                approvalPolicy,
                autoApproveReadOnly,
                requireApprovalForComments,
                requireApprovalForBranch,
                requireApprovalForPullRequest,
                requireApprovalForTaskMutation,
                defaultModelPolicy,
                archivedAt,
                createdAt,
                updatedAt);
    }
}
