package com.taskmind.ai.contracts.chat;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.UUID;

/** Nova-to-Core response for a completed chat turn. */
@JsonPropertyOrder({"sessionId", "message", "runId", "actions"})
public record ChatResponse(String sessionId, String message, UUID runId, List<ChatAction> actions) {}
