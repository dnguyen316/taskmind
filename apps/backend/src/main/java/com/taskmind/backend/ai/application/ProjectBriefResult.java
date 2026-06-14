package com.taskmind.backend.ai.application;

import java.util.List;
import java.util.UUID;

public record ProjectBriefResult(
        UUID projectId, String summary, List<String> currentFocus, List<String> risks, List<String> suggestedNextSteps) {}
