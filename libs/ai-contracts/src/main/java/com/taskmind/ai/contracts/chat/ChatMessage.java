package com.taskmind.ai.contracts.chat;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.time.Instant;

/** Persistable chat session message summary. */
@JsonPropertyOrder({"role", "content", "createdAt"})
public record ChatMessage(String role, String content, Instant createdAt) {}
