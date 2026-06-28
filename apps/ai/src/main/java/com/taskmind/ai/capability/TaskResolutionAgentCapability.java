package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class TaskResolutionAgentCapability implements Capability {
    public static final String PROMPT_VERSION = "task-resolution-agent.v1";
    private static final Set<String> SUPPORTED_TEMPLATE_IDS = Set.of("task-resolution-default", "github-task-resolution");
    private static final Set<String> SUPPORTED_TEMPLATE_VERSIONS = Set.of("1", "v1");
    private static final Set<String> SUPPORTED_TOOLS = Set.of("core.task.update", "core.task.comment", "core.github.issue.comment", "core.github.pull-request.create");
    private static final Set<String> APPROVAL_POLICIES = Set.of("propose-only", "require-approval");

    private final ObjectMapper objectMapper;

    public TaskResolutionAgentCapability(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public AiCapabilityId id() {
        return AiCapabilityId.TASK_RESOLUTION_AGENT;
    }

    @Override
    public String description() {
        return "Plan approved Core-routed tool calls for resolving a task.";
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object").put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("task").put("type", "object");
        properties.putObject("projectId").put("type", "string").put("minLength", 1);
        properties.putObject("githubRepository").put("type", "object");
        properties.putObject("workflowTemplate").put("type", "object");
        properties.putObject("allowedTools").put("type", "array");
        properties.putObject("approvalPolicy").put("type", "string");
        ArrayNode required = schema.putArray("required");
        required.add("task").add("projectId").add("githubRepository").add("workflowTemplate").add("allowedTools").add("approvalPolicy");
        return schema;
    }

    @Override
    public JsonNode outputSchema() {
        return AbstractTypedCapability.schema(objectMapper, "taskId", "workflowTemplateId", "workflowTemplateVersion", "approvalPolicy", "plan", "toolCalls", "warnings");
    }

    @Override
    public JsonNode buildProviderInput(JsonNode input) {
        if (input == null || !input.isObject()) {
            throw new IllegalArgumentException("task-resolution-agent requires object input");
        }
        requireObject(input, "task");
        requireText(input.path("task"), "id");
        requireText(input.path("task"), "title");
        requireText(input.path("task"), "description");
        requireText(input.path("task"), "status");
        requireText(input, "projectId");
        requireObject(input, "githubRepository");
        requireText(input.path("githubRepository"), "owner");
        requireText(input.path("githubRepository"), "name");
        requireText(input.path("githubRepository"), "defaultBranch");
        requireObject(input, "workflowTemplate");
        String templateId = requireText(input.path("workflowTemplate"), "id");
        String templateVersion = requireText(input.path("workflowTemplate"), "version");
        if (!SUPPORTED_TEMPLATE_IDS.contains(templateId) || !SUPPORTED_TEMPLATE_VERSIONS.contains(templateVersion)) {
            throw new IllegalArgumentException("Unsupported task-resolution workflow template '" + templateId + "@" + templateVersion + "'");
        }
        if (!input.path("allowedTools").isArray() || input.path("allowedTools").isEmpty()) {
            throw new IllegalArgumentException("task-resolution-agent requires at least one allowed tool");
        }
        for (JsonNode tool : input.path("allowedTools")) {
            String toolId = tool.asText("");
            if (!SUPPORTED_TOOLS.contains(toolId)) {
                throw new IllegalArgumentException("Unsupported task-resolution tool '" + toolId + "'");
            }
        }
        String approvalPolicy = requireText(input, "approvalPolicy");
        if (!APPROVAL_POLICIES.contains(approvalPolicy)) {
            throw new IllegalArgumentException("Unsupported task-resolution approval policy '" + approvalPolicy + "'");
        }
        ObjectNode normalized = input.deepCopy();
        normalized.put("promptVersion", PROMPT_VERSION);
        return normalized;
    }

    private void requireObject(JsonNode input, String field) {
        if (!input.path(field).isObject()) {
            throw new IllegalArgumentException("task-resolution-agent requires object field '" + field + "'");
        }
    }

    private String requireText(JsonNode input, String field) {
        String value = input.path(field).asText("");
        if (value.isBlank()) {
            throw new IllegalArgumentException("task-resolution-agent requires non-blank field '" + field + "'");
        }
        return value;
    }
}
