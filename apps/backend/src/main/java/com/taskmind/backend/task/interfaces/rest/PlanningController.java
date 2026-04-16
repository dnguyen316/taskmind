package com.taskmind.backend.task.interfaces.rest;

import com.taskmind.backend.task.application.TaskApplicationService;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@Validated
public class PlanningController {

    private final TaskApplicationService taskApplicationService;

    public PlanningController(TaskApplicationService taskApplicationService) {
        this.taskApplicationService = taskApplicationService;
    }

    @PostMapping("/ai/capture")
    public CaptureResponse capture(@Valid @RequestBody CaptureRequest request) {
        var drafts = new ArrayList<CapturedTaskDraft>();
        var lines = request.text().lines()
            .map(String::trim)
            .filter(line -> !line.isBlank())
            .limit(10)
            .toList();

        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            var confidence = Math.max(0.5d, 0.9d - (i * 0.05d));
            drafts.add(new CapturedTaskDraft(line, "TODO", 3, 30, "AI_CAPTURE", confidence));
        }

        String clarificationQuestion = drafts.isEmpty()
            ? "Could you share at least one action-oriented task?"
            : null;

        return new CaptureResponse(drafts, clarificationQuestion);
    }

    @PostMapping("/ai/goals/{goalId}/breakdown")
    public GoalBreakdownResponse breakdownGoal(
        @PathVariable UUID goalId,
        @Valid @RequestBody GoalBreakdownRequest request
    ) {
        var milestone = new GoalMilestone(
            "Milestone 1",
            request.deadline(),
            List.of("Define deliverables", "Plan weekly execution")
        );

        var task = new GoalDraftTask(
            "Immediate next action",
            "TODO",
            request.deadline() != null ? request.deadline().minusDays(6) : null,
            "Kick off work from goal breakdown",
            List.of()
        );

        return new GoalBreakdownResponse(goalId, List.of(milestone), List.of(task), List.of("Review generated dependencies before saving."));
    }

    @PostMapping("/planner/daily/generate")
    public DailyPlanResponse generateDailyPlan(@Valid @RequestBody DailyPlanRequest request) {
        var tasks = taskApplicationService.list(
            Optional.of(request.userId()),
            Optional.empty(),
            false,
            0,
            200
        );

        var ordered = tasks.stream()
            .filter(task -> task.status() != TaskStatus.DONE && task.status() != TaskStatus.ARCHIVED)
            .filter(task -> request.includeBlockedTasks() || task.status() != TaskStatus.TODO)
            .sorted(Comparator
                .comparingInt(Task::priority)
                .thenComparing(task -> Optional.ofNullable(task.dueAt()).orElse(OffsetDateTime.MAX)))
            .toList();

        int budget = request.availableMinutes();
        var selected = new ArrayList<PlannedTask>();
        var overflow = new ArrayList<PlannedTask>();

        for (Task task : ordered) {
            int duration = Optional.ofNullable(task.durationMinutes()).orElse(30);
            var planned = new PlannedTask(
                task.id(),
                task.title(),
                task.status().name(),
                duration,
                "Ranked by priority, due risk, and readiness"
            );

            if (budget - duration >= 0) {
                selected.add(planned);
                budget -= duration;
            } else {
                overflow.add(planned);
            }
        }

        return new DailyPlanResponse(selected, overflow, request.availableMinutes() - budget);
    }

    @PostMapping("/planner/reschedule/proposals")
    public RescheduleProposalsResponse generateRescheduleProposals(
        @Valid @RequestBody RescheduleProposalsRequest request
    ) {
        var proposals = request.taskIds().stream()
            .map(taskId -> new RescheduleProposal(
                taskId,
                "move",
                "Move by one day to reduce deadline collision.",
                List.of("Check downstream dependency order after applying.")
            ))
            .toList();

        return new RescheduleProposalsResponse(proposals, request.confirmationTokenRequired());
    }

    @PostMapping("/review/weekly/generate")
    public WeeklyReviewResponse generateWeeklyReview(@Valid @RequestBody WeeklyReviewRequest request) {
        return new WeeklyReviewResponse(
            request.userId(),
            "You completed core execution work and maintained steady throughput.",
            List.of("Two tasks slipped due to under-estimated duration."),
            List.of(
                "Reduce daily planned load by 15% for buffer.",
                "Front-load one high-impact task before noon.",
                "Review blockers at end-of-day and reschedule proactively."
            ),
            List.of("Priority: stabilize planner feedback loop")
        );
    }

    public record CaptureRequest(@NotBlank String text) { }

    public record CapturedTaskDraft(
        String title,
        String status,
        int priority,
        int durationMinutes,
        String source,
        double confidence
    ) { }

    public record CaptureResponse(List<CapturedTaskDraft> drafts, String clarificationQuestion) { }

    public record GoalBreakdownRequest(OffsetDateTime deadline, @Min(0) @Max(10080) Integer weeklyAvailabilityMinutes) { }

    public record GoalMilestone(String title, OffsetDateTime targetDate, List<String> notes) { }

    public record GoalDraftTask(
        String title,
        String status,
        OffsetDateTime dueAt,
        String rationale,
        List<String> dependencies
    ) { }

    public record GoalBreakdownResponse(
        UUID goalId,
        List<GoalMilestone> milestones,
        List<GoalDraftTask> tasks,
        List<String> riskNotes
    ) { }

    public record DailyPlanRequest(
        @NotNull UUID userId,
        @Min(1) @Max(1440) int availableMinutes,
        boolean includeBlockedTasks
    ) { }

    public record PlannedTask(
        UUID taskId,
        String title,
        String status,
        int durationMinutes,
        String rationale
    ) { }

    public record DailyPlanResponse(List<PlannedTask> plan, List<PlannedTask> overflow, int allocatedMinutes) { }

    public record RescheduleProposalsRequest(
        @NotNull List<UUID> taskIds,
        boolean confirmationTokenRequired
    ) { }

    public record RescheduleProposal(
        UUID taskId,
        String action,
        String explanation,
        List<String> conflictWarnings
    ) { }

    public record RescheduleProposalsResponse(
        List<RescheduleProposal> proposals,
        boolean confirmationTokenRequired
    ) { }

    public record WeeklyReviewRequest(@NotNull UUID userId) { }

    public record WeeklyReviewResponse(
        UUID userId,
        String summary,
        List<String> slippageInsights,
        List<String> recommendations,
        List<String> nextWeekPriorities
    ) { }
}
