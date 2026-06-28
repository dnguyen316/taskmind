package com.taskmind.ai.contracts.taskresolution;

import java.util.List;

public record TaskResolutionAgentRequest(
        TaskContext task,
        String projectId,
        GitHubRepositoryMetadata githubRepository,
        WorkflowTemplateSelection workflowTemplate,
        List<String> allowedTools,
        String approvalPolicy) {

    public record TaskContext(String id, String title, String description, String status) {}

    public record GitHubRepositoryMetadata(String owner, String name, String defaultBranch, String installationId) {}

    public record WorkflowTemplateSelection(String id, String version) {}
}
