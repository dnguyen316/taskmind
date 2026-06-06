package com.taskmind.ai.contracts.capability;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import com.taskmind.ai.contracts.AiRunStatus;
import java.util.List;
import java.util.UUID;

/** Nova-to-Core response for a capability execution. */
@JsonPropertyOrder({"runId", "status", "output", "warnings", "error"})
public record CapabilityResponse(
        UUID runId,
        AiRunStatus status,
        JsonNode output,
        List<String> warnings,
        CapabilityError error) {}
