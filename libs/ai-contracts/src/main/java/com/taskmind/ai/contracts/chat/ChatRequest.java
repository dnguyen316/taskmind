package com.taskmind.ai.contracts.chat;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** Core-to-Nova request for creating or continuing a chat turn. */
@JsonPropertyOrder({
    "sessionId",
    "message",
    "timezone",
    "locale",
    "correlationId",
    "projectId",
    "taskId",
    "scope"
})
public record ChatRequest(
        String sessionId,
        String message,
        String timezone,
        String locale,
        String correlationId,
        String projectId,
        String taskId,
        String scope) {}
