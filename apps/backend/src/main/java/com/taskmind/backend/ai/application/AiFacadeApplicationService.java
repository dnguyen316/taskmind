package com.taskmind.backend.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskmind.ai.contracts.AiCapabilityId;
import com.taskmind.ai.contracts.capability.CapabilityRequest;
import com.taskmind.backend.ai.NovaClient;
import com.taskmind.backend.ai.NovaClientException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiFacadeApplicationService {
    private final NovaClient novaClient;
    private final AiFacadeLocalFallbacks fallbacks;
    private final AiDomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public AiFacadeApplicationService(
            NovaClient novaClient,
            AiFacadeLocalFallbacks fallbacks,
            AiDomainEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.novaClient = novaClient;
        this.fallbacks = fallbacks;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    public CaptureResult capture(UUID userId, String text) {
        eventPublisher.publish(userId, "ai.capture_submitted", Map.of("length", text.length()));
        return runOrFallback(
                AiCapabilityId.CAPTURE,
                userId,
                Map.of("text", text),
                CaptureResult.class,
                () -> fallbacks.capture(text));
    }

    public GoalBreakdownResult goalBreakdown(
            UUID userId,
            UUID goalId,
            java.time.OffsetDateTime deadline,
            Integer weeklyAvailabilityMinutes) {
        return runOrFallback(
                AiCapabilityId.GOAL_BREAKDOWN,
                userId,
                Map.of(
                        "goalId",
                        goalId,
                        "deadline",
                        deadline == null ? "" : deadline.toString(),
                        "weeklyAvailabilityMinutes",
                        weeklyAvailabilityMinutes == null ? 0 : weeklyAvailabilityMinutes),
                GoalBreakdownResult.class,
                () -> fallbacks.goalBreakdown(goalId, deadline, weeklyAvailabilityMinutes));
    }

    public WeeklyReviewResult weeklyReview(UUID userId) {
        return runOrFallback(
                AiCapabilityId.WEEKLY_REVIEW,
                userId,
                Map.of("userId", userId),
                WeeklyReviewResult.class,
                () -> fallbacks.weeklyReview(userId));
    }

    public ProjectBriefResult projectBrief(
            UUID userId, UUID projectId, String name, String description) {
        return runOrFallback(
                AiCapabilityId.PROJECT_BRIEF,
                userId,
                Map.of(
                        "projectId",
                        projectId,
                        "name",
                        name == null ? "" : name,
                        "description",
                        description == null ? "" : description),
                ProjectBriefResult.class,
                () -> fallbacks.projectBrief(projectId, name, description));
    }

    public DurationEstimateResult durationEstimate(UUID userId, String title, String description) {
        return runOrFallback(
                AiCapabilityId.DURATION_ESTIMATE,
                userId,
                Map.of("title", title == null ? "" : title, "description", description == null ? "" : description),
                DurationEstimateResult.class,
                () -> fallbacks.durationEstimate(title, description));
    }

    public RationalePhraseResult rationalePhrase(UUID userId, String title, String context) {
        return runOrFallback(
                AiCapabilityId.RATIONALE_PHRASE,
                userId,
                Map.of("title", title == null ? "" : title, "context", context == null ? "" : context),
                RationalePhraseResult.class,
                () -> fallbacks.rationalePhrase(title, context));
    }

    public DashboardInsightsResult dashboardInsights(UUID userId) {
        return runOrFallback(
                AiCapabilityId.DASHBOARD_INSIGHTS,
                userId,
                Map.of("userId", userId),
                DashboardInsightsResult.class,
                () -> fallbacks.dashboardInsights(userId));
    }

    public DescribeTaskResult describe(UUID userId, String title, String notes) {
        return runOrFallback(
                new AiCapabilityId("describe-task"),
                userId,
                Map.of("title", title, "notes", notes == null ? "" : notes),
                DescribeTaskResult.class,
                () -> fallbacks.describe(title, notes));
    }

    public DescribeTaskAutocompleteResult autocomplete(UUID userId, String text) {
        return runOrFallback(
                new AiCapabilityId("autocomplete-task"),
                userId,
                Map.of("text", text),
                DescribeTaskAutocompleteResult.class,
                () -> fallbacks.autocomplete(text));
    }

    public TranslateTaskResult translate(UUID userId, String text, String targetLanguage) {
        return runOrFallback(
                new AiCapabilityId("translate-task"),
                userId,
                Map.of("text", text, "targetLanguage", targetLanguage),
                TranslateTaskResult.class,
                () -> fallbacks.translate(text, targetLanguage));
    }

    private <T> T runOrFallback(
            AiCapabilityId capabilityId,
            UUID userId,
            Map<String, Object> input,
            Class<T> type,
            Fallback<T> fallback) {
        try {
            JsonNode node = objectMapper.valueToTree(input);
            JsonNode output =
                    novaClient
                            .executeCapability(
                                    capabilityId.value(),
                                    new CapabilityRequest(
                                            capabilityId, userId, "default", node, null, null))
                            .output();
            if (!hasExpectedShape(output, type)) {
                return localFallback(type, fallback);
            }
            return withMetadata(output, type, AiResponseSource.NOVA, false);
        } catch (NovaClientException | IllegalArgumentException ex) {
            return localFallback(type, fallback);
        } catch (Exception ex) {
            return localFallback(type, fallback);
        }
    }

    private <T> T localFallback(Class<T> type, Fallback<T> fallback) {
        return withMetadata(objectMapper.valueToTree(fallback.get()), type, AiResponseSource.LOCAL_FALLBACK, true);
    }

    private <T> T withMetadata(JsonNode output, Class<T> type, AiResponseSource source, boolean degraded) {
        ObjectNode object = output.deepCopy();
        object.put("source", source.name());
        object.put("degraded", degraded);
        return objectMapper.convertValue(object, type);
    }

    private boolean hasExpectedShape(JsonNode output, Class<?> type) {
        if (output == null || !output.isObject()) {
            return false;
        }
        if (type.equals(CaptureResult.class)) {
            return output.has("drafts");
        }
        if (type.equals(GoalBreakdownResult.class)) {
            return output.has("milestones") && output.has("tasks");
        }
        if (type.equals(WeeklyReviewResult.class)) {
            return output.has("summary") && output.has("recommendations");
        }
        if (type.equals(ProjectBriefResult.class)) {
            return output.has("summary") && output.has("suggestedNextSteps");
        }
        if (type.equals(DurationEstimateResult.class)) {
            return output.has("durationMinutes") && output.has("rationale");
        }
        if (type.equals(RationalePhraseResult.class)) {
            return output.has("rationale");
        }
        if (type.equals(DashboardInsightsResult.class)) {
            return output.has("insights") && output.has("recommendations");
        }
        if (type.equals(DescribeTaskResult.class)) {
            return output.has("description");
        }
        if (type.equals(DescribeTaskAutocompleteResult.class)) {
            return output.has("suggestions");
        }
        if (type.equals(TranslateTaskResult.class)) {
            return output.has("translatedText");
        }
        return true;
    }

    @FunctionalInterface
    private interface Fallback<T> {
        T get();
    }
}
