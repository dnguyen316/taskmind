package com.taskmind.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;

public record ProviderRequest(
        AiCapabilityId capabilityId, JsonNode input, List<String> messages, String correlationId) {}
