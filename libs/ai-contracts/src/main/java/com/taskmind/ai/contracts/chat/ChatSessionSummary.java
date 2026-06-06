package com.taskmind.ai.contracts.chat;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;

/** Lightweight chat session metadata safe for Core facade responses. */
@JsonPropertyOrder({"sessionId", "lastMessageAt", "messageCount"})
public record ChatSessionSummary(String sessionId, Instant lastMessageAt, int messageCount) {}
