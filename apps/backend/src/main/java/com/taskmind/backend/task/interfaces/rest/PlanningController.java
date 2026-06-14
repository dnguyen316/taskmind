package com.taskmind.backend.task.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.ai.application.AiDomainEventPublisher;
import com.taskmind.backend.ai.application.AiFacadeApplicationService;
import com.taskmind.backend.task.application.CreateTaskCommand;
import com.taskmind.backend.task.application.TaskApplicationService;
import com.taskmind.backend.task.application.TaskLinkApplicationService;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskLink;
import com.taskmind.backend.task.domain.model.TaskLinkType;
import com.taskmind.backend.task.domain.model.TaskSource;
import com.taskmind.backend.task.domain.model.TaskStatus;
import com.taskmind.backend.task.interfaces.rest.PlanningController.CapturedTaskDraft;
import com.taskmind.backend.task.interfaces.rest.PlanningController.GoalDraftTask;
import com.taskmind.backend.task.interfaces.rest.PlanningController.GoalMilestone;
import com.taskmind.backend.task.interfaces.rest.PlanningController.PlannedTask;
import com.taskmind.backend.task.interfaces.rest.PlanningController.RescheduleProposal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.taskmind.events.EventTypes;

@RestController
@RequestMapping("/v1")
@Validated
public class PlanningController {

    private final TaskApplicationService taskApplicationService;
    private final TaskLinkApplicationService taskLinkApplicationService;
    private final AiFacadeApplicationService aiFacadeApplicationService;
    private final AiDomainEventPublisher aiDomainEventPublisher;

    public PlanningController(
            TaskApplicationService taskApplicationService,
            TaskLinkApplicationService taskLinkApplicationService,
            AiFacadeApplicationService aiFacadeApplicationService,
            AiDomainEventPublisher aiDomainEventPublisher) {
        this.taskApplicationService = taskApplicationService;
        this.taskLinkApplicationService = taskLinkApplicationService;
        this.aiFacadeApplicationService = aiFacadeApplicationService;
        this.aiDomainEventPublisher = aiDomainEventPublisher;
    }

    @PostMapping("/ai/capture")
    public CaptureResponse capture(
            AuthenticatedUser requester, @Valid @RequestBody CaptureRequest request) {
        com.taskmind.backend.ai.application.CaptureResult result =
                aiFacadeApplicationService.capture(requester.userId(), request.text());
        return new CaptureResponse(
                result.drafts().stream()
                        .map(draft -> new CapturedTaskDraft(
                                draft.title(),
                                draft.status(),
                                draft.priority(),
                                draft.durationMinutes(),
                                draft.source(),
                                draft.confidence()))
                        .toList(),
                result.clarificationQuestion());
    }


    @PostMapping("/ai/capture/accept")
    public CaptureAcceptResponse acceptCapturedDraft(
            AuthenticatedUser requester, @Valid @RequestBody CaptureAcceptRequest request) {
        Task created =
                taskApplicationService.create(
                        requester,
                        new CreateTaskCommand(
                                requester.userId(),
                                request.projectId(),
                                request.assigneeId(),
                                request.parentTaskId(),
                                null,
                                null,
                                null,
                                null,
                                request.draft().title(),
                                request.description(),
                                request.draft().status(),
                                request.draft().priority(),
                                request.dueAt(),
                                request.draft().durationMinutes(),
                                null,
                                TaskSource.AI_CAPTURE,
                                BigDecimal.valueOf(request.draft().confidence())));
        aiDomainEventPublisher.publish(
                requester.userId(),
                EventTypes.AI_SUGGESTION_ACCEPTED,
                Map.of(
                        "eventType", EventTypes.AI_SUGGESTION_ACCEPTED,
                        "taskId", created.id(),
                        "title", created.title(),
                        "source", created.source().name(),
                        "confidence", created.confidence()));
        return new CaptureAcceptResponse(created.id());
    }

    @PostMapping("/ai/capture/reject")
    public CaptureRejectResponse rejectCapturedDraft(
            AuthenticatedUser requester, @Valid @RequestBody CaptureRejectRequest request) {
        aiDomainEventPublisher.publish(
                requester.userId(),
                EventTypes.AI_SUGGESTION_REJECTED,
                Map.of(
                        "eventType", EventTypes.AI_SUGGESTION_REJECTED,
                        "title", request.draft().title(),
                        "reason", request.reason(),
                        "confidence", request.draft().confidence()));
        return new CaptureRejectResponse(true);
    }

    @PostMapping("/ai/tasks/describe")
    public DescribeTaskResponse describeTask(
            AuthenticatedUser requester, @Valid @RequestBody DescribeTaskRequest request) {
        com.taskmind.backend.ai.application.DescribeTaskResult result =
                aiFacadeApplicationService.describe(
                        requester.userId(), request.title(), request.notes());
        return new DescribeTaskResponse(result.description(), result.rationale());
    }

    @PostMapping("/ai/tasks/describe/autocomplete")
    public DescribeTaskAutocompleteResponse autocompleteTask(
            AuthenticatedUser requester,
            @Valid @RequestBody DescribeTaskAutocompleteRequest request) {
        com.taskmind.backend.ai.application.DescribeTaskAutocompleteResult result =
                aiFacadeApplicationService.autocomplete(requester.userId(), request.text());
        return new DescribeTaskAutocompleteResponse(result.suggestions());
    }

    @PostMapping("/ai/tasks/translate")
    public TranslateTaskResponse translateTask(
            AuthenticatedUser requester, @Valid @RequestBody TranslateTaskRequest request) {
        com.taskmind.backend.ai.application.TranslateTaskResult result =
                aiFacadeApplicationService.translate(
                        requester.userId(), request.text(), request.targetLanguage());
        return new TranslateTaskResponse(result.translatedText(), result.targetLanguage());
    }

    @PostMapping("/ai/goals/{goalId}/breakdown")
    public GoalBreakdownResponse breakdownGoal(
            AuthenticatedUser requester,
            @PathVariable UUID goalId,
            @Valid @RequestBody GoalBreakdownRequest request) {
        com.taskmind.backend.ai.application.GoalBreakdownResult result =
                aiFacadeApplicationService.goalBreakdown(
                        requester.userId(), goalId, request.deadline(), request.weeklyAvailabilityMinutes());
        return new GoalBreakdownResponse(
                result.goalId(),
                result.milestones().stream()
                        .map(milestone -> new GoalMilestone(milestone.title(), milestone.targetDate(), milestone.notes()))
                        .toList(),
                result.tasks().stream()
                        .map(task -> new GoalDraftTask(task.title(), task.status(), task.dueAt(), task.rationale(), task.dependencies()))
                        .toList(),
                result.riskNotes());
    }

    @PostMapping("/planner/daily/generate")
    public DailyPlanResponse generateDailyPlan(
            AuthenticatedUser requester, @Valid @RequestBody DailyPlanRequest request) {
        List<Task> tasks =
                taskApplicationService.list(
                        requester, Optional.of(request.userId()), Optional.empty(), false, 0, 200);

        List<Task> ordered =
                tasks.stream()
                        .filter(
                                task ->
                                        task.status() != TaskStatus.DONE
                                                && task.status() != TaskStatus.ARCHIVED)
                        .filter(task -> task.status() == TaskStatus.TODO || task.status() == TaskStatus.IN_PROGRESS)
                        .filter(
                                task ->
                                        request.includeBlockedTasks()
                                                || !isBlockedByIncompleteDependency(requester, task))
                        .sorted(
                                Comparator.comparingInt(Task::priority)
                                        .thenComparing(
                                                task ->
                                                        Optional.ofNullable(task.dueAt())
                                                                .orElse(OffsetDateTime.MAX)))
                        .toList();

        int budget = request.availableMinutes();
        ArrayList<PlannedTask> selected = new ArrayList<PlannedTask>();
        ArrayList<PlannedTask> overflow = new ArrayList<PlannedTask>();

        for (Task task : ordered) {
            int duration = Optional.ofNullable(task.durationMinutes()).orElse(30);
            PlannedTask planned =
                    new PlannedTask(
                            task.id(),
                            task.title(),
                            task.status().name(),
                            duration,
                            "Ranked by priority, due risk, and readiness");

            if (budget - duration >= 0) {
                selected.add(planned);
                budget -= duration;
            } else {
                overflow.add(planned);
            }
        }

        return new DailyPlanResponse(selected, overflow, request.availableMinutes() - budget);
    }

    private boolean isBlockedByIncompleteDependency(AuthenticatedUser requester, Task task) {
        return taskLinkApplicationService.list(requester, task.id()).stream()
                .filter(link -> link.linkType() == TaskLinkType.BLOCKS)
                .filter(link -> task.id().equals(link.targetTaskId()))
                .map(TaskLink::sourceTaskId)
                .map(dependencyId -> taskApplicationService.findById(requester, dependencyId))
                .flatMap(Optional::stream)
                .anyMatch(dependency -> dependency.status() != TaskStatus.DONE
                        && dependency.status() != TaskStatus.ARCHIVED);
    }

    @PostMapping("/planner/reschedule/proposals")
    public RescheduleProposalsResponse generateRescheduleProposals(
            AuthenticatedUser requester, @Valid @RequestBody RescheduleProposalsRequest request) {
        List<RescheduleProposal> proposals =
                request.taskIds().stream()
                        .map(
                                taskId -> {
                                    Task task =
                                            taskApplicationService
                                                    .findById(requester, taskId)
                                                    .orElseThrow(
                                                            () ->
                                                                    new IllegalArgumentException(
                                                                            "Task not found or access denied"));
                                    return new RescheduleProposal(
                                            task.id(),
                                            "move",
                                            "Move by one day to reduce deadline collision.",
                                            List.of(
                                                    "Check downstream dependency order after applying."));
                                })
                        .toList();

        return new RescheduleProposalsResponse(proposals, request.confirmationTokenRequired());
    }

    @PostMapping("/review/weekly/generate")
    public WeeklyReviewResponse generateWeeklyReview(
            AuthenticatedUser requester, @Valid @RequestBody WeeklyReviewRequest request) {
        com.taskmind.backend.ai.application.WeeklyReviewResult result =
                aiFacadeApplicationService.weeklyReview(requester.userId());
        return new WeeklyReviewResponse(
                result.userId(),
                result.summary(),
                result.slippageInsights(),
                result.recommendations(),
                result.nextWeekPriorities());
    }

    public record CaptureRequest(@NotBlank String text) {}

    public record CapturedTaskDraft(
            String title,
            String status,
            int priority,
            int durationMinutes,
            String source,
            double confidence) {}

    public record CaptureResponse(List<CapturedTaskDraft> drafts, String clarificationQuestion) {}


    public record CaptureAcceptRequest(
            @NotNull AcceptedTaskDraft draft,
            UUID projectId,
            UUID assigneeId,
            UUID parentTaskId,
            String description,
            OffsetDateTime dueAt) {}

    public record AcceptedTaskDraft(
            @NotBlank String title,
            @NotNull TaskStatus status,
            @Min(1) @Max(4) int priority,
            @Min(1) int durationMinutes,
            @DecimalMin("0.0") @DecimalMax("1.0") double confidence) {}

    public record CaptureAcceptResponse(UUID taskId) {}

    public record CaptureRejectRequest(@NotNull AcceptedTaskDraft draft, @NotBlank String reason) {}

    public record CaptureRejectResponse(boolean rejected) {}

    public record DescribeTaskRequest(@NotBlank String title, String notes) {}

    public record DescribeTaskResponse(String description, String rationale) {}

    public record DescribeTaskAutocompleteRequest(@NotBlank String text) {}

    public record DescribeTaskAutocompleteResponse(List<String> suggestions) {}

    public record TranslateTaskRequest(@NotBlank String text, @NotBlank String targetLanguage) {}

    public record TranslateTaskResponse(String translatedText, String targetLanguage) {}

    public record GoalBreakdownRequest(
            OffsetDateTime deadline, @Min(0) @Max(10080) Integer weeklyAvailabilityMinutes) {}

    public record GoalMilestone(String title, OffsetDateTime targetDate, List<String> notes) {}

    public record GoalDraftTask(
            String title,
            String status,
            OffsetDateTime dueAt,
            String rationale,
            List<String> dependencies) {}

    public record GoalBreakdownResponse(
            UUID goalId,
            List<GoalMilestone> milestones,
            List<GoalDraftTask> tasks,
            List<String> riskNotes) {}

    public record DailyPlanRequest(
            @NotNull UUID userId,
            @Min(1) @Max(1440) int availableMinutes,
            boolean includeBlockedTasks) {}

    public record PlannedTask(
            UUID taskId, String title, String status, int durationMinutes, String rationale) {}

    public record DailyPlanResponse(
            List<PlannedTask> plan, List<PlannedTask> overflow, int allocatedMinutes) {}

    public record RescheduleProposalsRequest(
            @NotNull List<UUID> taskIds, boolean confirmationTokenRequired) {}

    public record RescheduleProposal(
            UUID taskId, String action, String explanation, List<String> conflictWarnings) {}

    public record RescheduleProposalsResponse(
            List<RescheduleProposal> proposals, boolean confirmationTokenRequired) {}

    public record WeeklyReviewRequest() {}

    public record WeeklyReviewResponse(
            UUID userId,
            String summary,
            List<String> slippageInsights,
            List<String> recommendations,
            List<String> nextWeekPriorities) {}
}
