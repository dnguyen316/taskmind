package com.taskmind.ai.contracts.taskresolution;

import java.util.List;

public record TaskResolutionAgentResponse(
        String taskId,
        String workflowTemplateId,
        String workflowTemplateVersion,
        String approvalPolicy,
        List<TaskResolutionPlanStep> plan,
        List<TaskResolutionToolCall> toolCalls,
        List<String> warnings) {}
