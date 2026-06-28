package com.taskmind.ai.contracts.taskresolution;

import java.util.List;

public record TaskResolutionActionProposal(String actionId, String title, String rationale, List<TaskResolutionToolCall> toolCalls) {}
