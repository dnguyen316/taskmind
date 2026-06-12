package com.taskmind.ai.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskmind.ai.contracts.AiCapabilityId;
import com.taskmind.ai.contracts.AiProviderId;
import java.util.UUID;

public record AiRunRecord(
        UUID userId,
        String workspaceId,
        AiCapabilityId capabilityId,
        AiProviderId providerId,
        String modelId,
        String requestHash,
        JsonNode input,
        String correlationId) {}
