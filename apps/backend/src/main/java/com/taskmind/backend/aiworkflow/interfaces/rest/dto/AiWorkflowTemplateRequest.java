package com.taskmind.backend.aiworkflow.interfaces.rest.dto;

import com.taskmind.backend.aiworkflow.application.AiWorkflowTemplateApplicationService.TemplateCommand;
import com.taskmind.backend.aiworkflow.domain.model.ApprovalPolicy;
import com.taskmind.backend.aiworkflow.domain.model.WorkflowType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiWorkflowTemplateRequest(
        @NotBlank String name,
        String description,
        @NotNull WorkflowType workflowType,
        @NotBlank String templateBody,
        String allowedTools,
        @NotNull ApprovalPolicy approvalPolicy,
        String defaultModelPolicy,
        Long version) {
    public TemplateCommand toCommand() {
        return new TemplateCommand(name, description, workflowType, templateBody, allowedTools, approvalPolicy,
                defaultModelPolicy, version);
    }
}
