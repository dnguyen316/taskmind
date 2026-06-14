package com.taskmind.backend.ai.application;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AiFacadeLocalFallbacks {
    public CaptureResult capture(String text) {
        List<String> lines = text.lines().map(String::trim).filter(line -> !line.isBlank()).limit(10).toList();
        ArrayList<CaptureResult.CapturedTaskDraft> drafts = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            drafts.add(new CaptureResult.CapturedTaskDraft(
                    lines.get(i), "TODO", 3, 30, "AI_CAPTURE", Math.max(0.5d, 0.9d - (i * 0.05d))));
        }
        return new CaptureResult(drafts, drafts.isEmpty() ? "Could you share at least one action-oriented task?" : null);
    }

    public GoalBreakdownResult goalBreakdown(UUID goalId, OffsetDateTime deadline, Integer weeklyAvailabilityMinutes) {
        OffsetDateTime target = deadline != null ? deadline : OffsetDateTime.now().plusWeeks(2);
        return new GoalBreakdownResult(
                goalId,
                List.of(new GoalBreakdownResult.GoalMilestone("Plan and validate scope", target.minusDays(7), List.of("Confirm success criteria", "Identify dependencies"))),
                List.of(new GoalBreakdownResult.GoalDraftTask("Define the next concrete action", "TODO", target.minusDays(6), "First locally generated step toward the goal.", List.of())),
                List.of("Review generated dates against team capacity (" + (weeklyAvailabilityMinutes == null ? "unknown" : weeklyAvailabilityMinutes) + " minutes/week)."));
    }

    public WeeklyReviewResult weeklyReview(UUID userId) {
        return new WeeklyReviewResult(userId, "You maintained steady execution and have a clear next planning loop.", List.of("Some work may need tighter estimates."), List.of("Reserve buffer before committing each day.", "Review blockers before rescheduling."), List.of("Stabilize the highest-priority active project."));
    }

    public ProjectBriefResult projectBrief(UUID projectId, String name, String description) {
        return new ProjectBriefResult(projectId, "Project " + safe(name) + " is ready for focused execution.", List.of("Clarify deliverables", "Sequence the next milestone"), List.of("Unreviewed assumptions may affect scope."), List.of("Confirm owner", "Create the next three tasks"));
    }

    public DurationEstimateResult durationEstimate(String title, String description) {
        int minutes = Math.max(15, Math.min(240, (safe(title).length() + safe(description).length()) / 2 + 30));
        return new DurationEstimateResult(minutes, "Estimated locally from task complexity and text length.", 0.65d);
    }

    public RationalePhraseResult rationalePhrase(String title, String context) {
        return new RationalePhraseResult("Scheduled to protect focus for " + safe(title) + ".");
    }

    public DashboardInsightsResult dashboardInsights(UUID userId) {
        return new DashboardInsightsResult(userId, "Your dashboard is ready for review.", List.of("Prioritize active work with near-term due dates."), List.of("Keep daily plans below available capacity."));
    }

    public DescribeTaskResult describe(String title, String notes) {
        String suffix = notes == null || notes.isBlank() ? "" : " Notes: " + notes.trim();
        return new DescribeTaskResult("Complete: " + title.trim() + "." + suffix, "Generated locally from the task title and notes.");
    }

    public DescribeTaskAutocompleteResult autocomplete(String text) {
        String base = text == null || text.isBlank() ? "Add acceptance criteria" : text.trim();
        return new DescribeTaskAutocompleteResult(List.of(base + " with clear acceptance criteria.", base + " and identify blockers."));
    }

    public TranslateTaskResult translate(String text, String targetLanguage) {
        return new TranslateTaskResult("[" + targetLanguage + "] " + text.trim(), targetLanguage);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "this work" : value.trim();
    }
}
