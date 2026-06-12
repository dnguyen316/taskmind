package com.taskmind.ai.chat;

import com.taskmind.ai.contracts.chat.ChatMessage;
import java.util.ArrayList;
import java.util.List;

public record ChatSession(String sessionId, List<ChatMessage> messages) {
    public ChatSession {
        messages = new ArrayList<>(messages == null ? List.of() : messages);
    }
}
