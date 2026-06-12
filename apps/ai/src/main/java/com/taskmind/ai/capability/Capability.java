package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskmind.ai.contracts.AiCapabilityId;

public interface Capability {
    AiCapabilityId id();

    String description();

    JsonNode inputSchema();

    JsonNode outputSchema();

    JsonNode buildProviderInput(JsonNode input);
}
