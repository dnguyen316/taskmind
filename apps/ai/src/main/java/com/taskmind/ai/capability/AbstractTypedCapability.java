package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;

abstract class AbstractTypedCapability implements Capability {
    private final AiCapabilityId id;
    private final String description;
    protected final ObjectMapper objectMapper;
    private final List<String> requiredTextFields;
    private final JsonNode outputSchema;

    AbstractTypedCapability(
            AiCapabilityId id,
            String description,
            ObjectMapper objectMapper,
            List<String> requiredTextFields,
            JsonNode outputSchema) {
        this.id = id;
        this.description = description;
        this.objectMapper = objectMapper;
        this.requiredTextFields = requiredTextFields;
        this.outputSchema = outputSchema;
    }

    @Override
    public AiCapabilityId id() {
        return id;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public JsonNode inputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", true);
        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");
        requiredTextFields.forEach(
                field -> {
                    properties.putObject(field).put("type", "string").put("minLength", 1);
                    required.add(field);
                });
        return schema;
    }

    @Override
    public JsonNode outputSchema() {
        return outputSchema;
    }

    @Override
    public JsonNode buildProviderInput(JsonNode input) {
        ObjectNode normalized =
                input == null || input.isNull()
                        ? objectMapper.createObjectNode()
                        : input.deepCopy();
        requiredTextFields.forEach(field -> requireNonBlank(normalized, field));
        return normalized;
    }

    protected static JsonNode schema(ObjectMapper objectMapper, String... requiredFields) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");
        for (String field : requiredFields) {
            properties.putObject(field);
            required.add(field);
        }
        return schema;
    }

    private void requireNonBlank(ObjectNode input, String field) {
        JsonNode value = input.get(field);
        if (value == null || value.isNull() || value.asText("").isBlank()) {
            throw new IllegalArgumentException(
                    id.value() + " requires non-blank input field '" + field + "'");
        }
    }
}
