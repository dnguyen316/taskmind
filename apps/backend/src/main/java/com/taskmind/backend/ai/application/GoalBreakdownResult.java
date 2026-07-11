package com.taskmind.backend.ai.application;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record GoalBreakdownResult(
        UUID goalId, List<GoalMilestone> milestones, List<GoalDraftTask> tasks, List<String> riskNotes, AiResponseSource source, boolean degraded) {
    public record GoalMilestone(String title, OffsetDateTime targetDate, List<String> notes) {}

    public record GoalDraftTask(
            String title, String status, OffsetDateTime dueAt, String rationale, List<String> dependencies) {}
}
