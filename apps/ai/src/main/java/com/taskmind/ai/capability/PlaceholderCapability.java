package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskmind.ai.contracts.AiCapabilityId;

public class PlaceholderCapability implements Capability {
    private final AiCapabilityId id;
    private final String description;
    private final ObjectMapper objectMapper;

    public PlaceholderCapability(AiCapabilityId id, String description, ObjectMapper objectMapper) {
        this.id = id;
        this.description = description;
        this.objectMapper = objectMapper;
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
        return schema;
    }

    @Override
    public JsonNode outputSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.put("additionalProperties", true);
        return schema;
    }

    @Override
    public JsonNode buildProviderInput(JsonNode input) {
        return input == null || input.isNull() ? objectMapper.createObjectNode() : input;
    }
}
