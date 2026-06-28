package com.taskmind.ai.contracts.taskresolution;

import com.fasterxml.jackson.databind.JsonNode;

public record TaskResolutionToolCall(String toolId, String coreInternalEndpoint, String method, JsonNode arguments) {}
