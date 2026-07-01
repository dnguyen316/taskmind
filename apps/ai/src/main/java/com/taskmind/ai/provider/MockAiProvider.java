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
                output.put(
                        "summary",
                        "You made steady progress and should protect planning buffer next week.");
                output.putArray("slippageInsights")
                        .add("Estimate variance is the main review signal.");
                output.putArray("recommendations")
                        .add("Limit daily commitments to available focus time.");
                output.putArray("nextWeekPriorities")
                        .add("Prioritize the most blocked active project.");
            }
            case "project-brief" -> {
                output.put(
                        "projectId",
                        text(input, "projectId", "00000000-0000-0000-0000-000000000000"));
                output.put(
                        "summary",
                        "Project "
                                + text(input, "name", "work")
                                + " is ready for focused execution.");
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
                output.put(
                        "translatedText",
                        "["
                                + text(input, "targetLanguage", "target")
                                + "] "
                                + text(input, "text", ""));
                output.put("targetLanguage", text(input, "targetLanguage", "target"));
            }
            case "duration-estimate" -> {
                int minutes =
                        Math.max(15, Math.min(240, text(input, "title", "").length() * 3 + 30));
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

            case "spec-outline" -> {
                String title = firstMeaningfulLine(text(input, "specText", "Spec workspace"));
                var epics = output.putArray("epics");
                ObjectNode epic = epics.addObject();
                epic.put("title", "Epic: " + title);
                epic.put("issueType", "EPIC");
                var stories = output.putArray("stories");
                ObjectNode story = stories.addObject();
                story.put("title", "Story: deliver " + title);
                story.put("parentTitle", epic.get("title").asText());
                story.put("issueType", "STORY");
                output.putArray("warnings");
            }
            case "spec-enrich" -> {
                var items = output.putArray("items");
                ObjectNode item = items.addObject();
                item.put("title", firstMeaningfulLine(text(input, "specText", "Spec item")));
                item.putArray("acceptanceCriteria")
                        .add("User can review the generated draft before materialization.");
                item.put("estimatePoints", 3);
                output.putArray("risks").add("Validate assumptions with the product owner.");
                output.putArray("labels").add("ai-generated").add("spec-breakdown");
            }
            case "spec-breakdown" -> {
                String title = firstMeaningfulLine(text(input, "specText", "Spec workspace"));
                var tree = output.putArray("tree");
                ObjectNode epic = tree.addObject();
                epic.put("level", "EPIC");
                epic.put("title", "Epic: " + title);
                var stories = epic.putArray("children");
                ObjectNode story = stories.addObject();
                story.put("level", "STORY");
                story.put("title", "Story: implement " + title);
                var subtasks = story.putArray("children");
                ObjectNode subtask = subtasks.addObject();
                subtask.put("level", "SUBTASK");
                subtask.put("title", "Subtask: verify " + title);
                output.putObject("metadata").put("template", text(input, "templateKey", "default"));
                output.putArray("warnings");
            }
            case "spec-breakdown-section" -> {
                String section =
                        firstMeaningfulLine(text(input, "sectionText", "Selected section"));
                output.put("sectionTitle", section);
                var items = output.putArray("items");
                ObjectNode item = items.addObject();
                item.put("level", "STORY");
                item.put("title", "Story: " + section);
                item.putArray("acceptanceCriteria")
                        .add("Section behavior is captured as reviewable work.");
                output.putArray("warnings");
            }
            case "spec-merge" -> {
                output.set("mergedTree", input.path("draftTree").deepCopy());
                output.putArray("conflicts");
                output.putArray("warnings")
                        .add("Merged deterministically; review before materializing tasks.");
            }
            case "spec-suggest-links" -> {
                var links = output.putArray("links");
                ObjectNode link = links.addObject();
                link.put("type", "doc");
                link.put(
                        "title",
                        "Reference for " + firstMeaningfulLine(text(input, "specText", "spec")));
                link.put("confidence", 0.71d);
                output.putArray("dependencies").add("Confirm upstream API availability.");
                output.putArray("warnings");
            }

            case "task-resolution-agent" -> {
                output.put("taskId", text(input.path("task"), "id", "task-unknown"));
                output.put(
                        "workflowTemplateId",
                        text(input.path("workflowTemplate"), "id", "task-resolution-default"));
                output.put(
                        "workflowTemplateVersion",
                        text(input.path("workflowTemplate"), "version", "1"));
                output.put("approvalPolicy", text(input, "approvalPolicy", "propose-only"));
                var proposals = output.putArray("proposals");
                ObjectNode proposal = proposals.addObject();
                String policy = text(input, "approvalPolicy", "propose-only");
                var toolCalls = output.putArray("toolCalls");
                ObjectNode toolCall = toolCalls.addObject();
                String toolId = input.path("allowedTools").path(0).asText("core.task.comment");
                proposal.put("proposedActionType", toolId);
                proposal.put("riskLevel", policy.equals("require-approval") ? "HIGH" : "LOW");
                proposal.put(
                        "rationale",
                        "Approval gate blocks direct execution until Core records a decision.");
                ObjectNode preview = proposal.putObject("payloadPreview");
                preview.put("taskId", text(input.path("task"), "id", "task-unknown"));
                preview.put("message", "Deterministic resolution proposal " + fingerprint);
                toolCall.put("toolId", toolId);
                toolCall.put("coreInternalEndpoint", coreEndpoint(toolId));
                toolCall.put("method", "POST");
                ObjectNode arguments = toolCall.putObject("arguments");
                arguments.put("taskId", text(input.path("task"), "id", "task-unknown"));
                arguments.put("message", "Deterministic resolution proposal " + fingerprint);
                var plan = output.putArray("plan");
                ObjectNode step = plan.addObject();
                step.put("order", 1);
                step.put("title", "Resolve task through Core");
                step.put("description", "Use approved Core internal endpoints only.");
                var actions = step.putArray("actions");
                ObjectNode action = actions.addObject();
                action.put("actionId", "propose-core-tool-call");
                action.put("title", "Prepare " + toolId);
                action.put(
                        "rationale",
                        "Tool calls are routed back through Core for policy enforcement.");
                action.set("toolCalls", toolCalls.deepCopy());
                output.putArray("warnings");
            }
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

    private String coreEndpoint(String toolId) {
        return switch (toolId) {
            case "core.task.update" -> "/internal/v1/tasks/update";
            case "core.github.issue.comment" -> "/internal/v1/integrations/github/issues/comment";
            case "core.github.pull-request.create" ->
                    "/internal/v1/integrations/github/pull-requests";
            default -> "/internal/v1/tasks/comment";
        };
    }

    private String firstMeaningfulLine(String text) {
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                return line.trim();
            }
        }
        return "Spec workspace";
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
