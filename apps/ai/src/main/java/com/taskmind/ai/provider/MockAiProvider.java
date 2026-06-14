package com.taskmind.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskmind.ai.contracts.AiProviderId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockAiProvider implements AiProvider {
    private final String modelId;
    private final ObjectMapper objectMapper;

    public MockAiProvider(
            @Value("${taskmind.ai.provider.mock.model-id:nova-mock-v1}") String modelId,
            ObjectMapper objectMapper) {
        this.modelId = modelId;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiProviderId id() {
        return AiProviderId.MOCK;
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public ProviderResponse complete(ProviderRequest request) {
        String canonical =
                request.capabilityId().value()
                        + ":"
                        + stableJson(request.input())
                        + ":"
                        + request.messages();
        String fingerprint = sha256(canonical).substring(0, 12);
        ObjectNode output = capabilityOutput(request, fingerprint);
        int promptTokens = Math.max(1, canonical.length() / 4);
        int completionTokens = 8;
        return new ProviderResponse(
                "Mock assistant response " + fingerprint,
                output,
                promptTokens,
                completionTokens,
                promptTokens + completionTokens,
                0L);
    }

    private ObjectNode capabilityOutput(ProviderRequest request, String fingerprint) {
        ObjectNode output = objectMapper.createObjectNode();
        String capability = request.capabilityId().value();
        output.put("provider", id().value());
        output.put("model", modelId);
        output.put("capabilityId", capability);
        output.put("fingerprint", fingerprint);
        JsonNode input = request.input();
        switch (capability) {
            case "capture" -> {
                var drafts = output.putArray("drafts");
                String text = text(input, "text", "Review captured work");
                for (String line : text.split("\\R")) {
                    if (!line.isBlank()) {
                        ObjectNode draft = drafts.addObject();
                        draft.put("title", line.trim());
                        draft.put("status", "TODO");
                        draft.put("priority", 3);
                        draft.put("durationMinutes", 30);
                        draft.put("source", "AI_CAPTURE");
                        draft.put("confidence", 0.82d);
                    }
                }
                output.putNull("clarificationQuestion");
            }
            case "goal-breakdown" -> {
                output.put("goalId", text(input, "goalId", "00000000-0000-0000-0000-000000000000"));
                var milestones = output.putArray("milestones");
                ObjectNode milestone = milestones.addObject();
                milestone.put("title", "Validate scope");
                milestone.put("targetDate", text(input, "deadline", ""));
                milestone.putArray("notes").add("Confirm deliverables").add("Sequence execution");
                var tasks = output.putArray("tasks");
                ObjectNode task = tasks.addObject();
                task.put("title", "Define immediate next action");
                task.put("status", "TODO");
                task.put("dueAt", text(input, "deadline", ""));
                task.put("rationale", "First deterministic step toward the goal.");
                task.putArray("dependencies");
                output.putArray("riskNotes").add("Review capacity before accepting.");
            }
            case "weekly-review" -> {
                output.put("userId", text(input, "userId", "00000000-0000-0000-0000-000000000000"));
                output.put("summary", "You made steady progress and should protect planning buffer next week.");
                output.putArray("slippageInsights").add("Estimate variance is the main review signal.");
                output.putArray("recommendations").add("Limit daily commitments to available focus time.");
                output.putArray("nextWeekPriorities").add("Prioritize the most blocked active project.");
            }
            case "project-brief" -> {
                output.put("projectId", text(input, "projectId", "00000000-0000-0000-0000-000000000000"));
                output.put("summary", "Project " + text(input, "name", "work") + " is ready for focused execution.");
                output.putArray("currentFocus").add("Clarify deliverables");
                output.putArray("risks").add("Scope assumptions need review.");
                output.putArray("suggestedNextSteps").add("Create the next three tasks.");
            }
            case "describe-task" -> {
                output.put("description", "Complete: " + text(input, "title", "task") + ".");
                output.put("rationale", "Generated by deterministic Nova mock.");
            }
            case "autocomplete-task" ->
                    output.putArray("suggestions")
                            .add(text(input, "text", "Add details") + " with acceptance criteria.")
                            .add("Identify blockers and owner.");
            case "translate-task" -> {
                output.put("translatedText", "[" + text(input, "targetLanguage", "target") + "] " + text(input, "text", ""));
                output.put("targetLanguage", text(input, "targetLanguage", "target"));
            }
            case "duration-estimate" -> {
                int minutes = Math.max(15, Math.min(240, text(input, "title", "").length() * 3 + 30));
                output.put("durationMinutes", minutes);
                output.put("rationale", "Estimated from deterministic task complexity signals.");
                output.put("confidence", 0.74d);
            }
            case "rationale-phrase" ->
                    output.put(
                            "rationale",
                            "Scheduled to protect focus for "
                                    + text(input, "title", "this task")
                                    + ".");
            case "dashboard-insights" -> {
                output.put("userId", text(input, "userId", "00000000-0000-0000-0000-000000000000"));
                output.put("summary", "Focus on overdue and high-priority work first.");
                output.putArray("insights").add("Capacity risk is visible in active work.");
                output.putArray("recommendations").add("Plan a smaller daily load.");
            }
            default -> {
                output.put("provider", id().value());
                output.put("model", modelId);
                output.put("capabilityId", capability);
                output.put("fingerprint", fingerprint);
                output.put("summary", "Mock result " + fingerprint);
            }
        }
        return output;
    }

    private String text(JsonNode input, String field, String fallback) {
        JsonNode value = input == null ? null : input.get(field);
        return value == null || value.isNull() ? fallback : value.asText(fallback);
    }

    private String stableJson(JsonNode input) {
        if (input == null || input.isNull()) {
            return "null";
        }
        return input.toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
