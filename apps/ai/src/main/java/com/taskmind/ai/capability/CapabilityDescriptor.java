package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.JsonNode;

public record CapabilityDescriptor(
        String id, String description, JsonNode inputSchema, JsonNode outputSchema) {}
