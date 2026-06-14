package com.taskmind.ai.contracts.capability;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;

/** Provider-neutral capability metadata safe for Core facade responses. */
@JsonPropertyOrder({"id", "description", "inputSchema", "outputSchema"})
public record CapabilityDescriptor(
        String id, String description, JsonNode inputSchema, JsonNode outputSchema) {}
