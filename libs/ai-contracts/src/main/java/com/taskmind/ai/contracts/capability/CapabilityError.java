package com.taskmind.ai.contracts.capability;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;

/** Stable Nova error shape that does not expose provider-specific internals. */
@JsonPropertyOrder({"code", "message", "details"})
public record CapabilityError(String code, String message, JsonNode details) {}
