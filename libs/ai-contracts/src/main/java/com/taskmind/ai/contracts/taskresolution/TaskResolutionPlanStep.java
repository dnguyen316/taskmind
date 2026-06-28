package com.taskmind.ai.contracts.taskresolution;

import java.util.List;

public record TaskResolutionPlanStep(int order, String title, String description, List<TaskResolutionActionProposal> actions) {}
