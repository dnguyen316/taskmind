package com.taskmind.ai.contracts.chat;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;

/** Structured assistant action metadata returned by Nova without mutating Core state. */
@JsonPropertyOrder({"type", "payload"})
public record ChatAction(String type, JsonNode payload) {}
