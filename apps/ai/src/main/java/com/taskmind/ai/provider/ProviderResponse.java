package com.taskmind.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;

public record ProviderResponse(
        String message,
        JsonNode output,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        long latencyMs) {}
