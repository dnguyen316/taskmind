package com.taskmind.ai.contracts.capability;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.UUID;

/** Core-to-Nova request for executing a registered capability. */
@JsonPropertyOrder({
    "capabilityId",
    "userId",
    "workspaceId",
    "input",
    "correlationId",
    "idempotencyKey"
})
public record CapabilityRequest(
        AiCapabilityId capabilityId,
        UUID userId,
        String workspaceId,
        JsonNode input,
        String correlationId,
        String idempotencyKey) {}
