package com.taskmind.ai.chat;

import java.util.Optional;

public interface ChatSessionStore {
    Optional<ChatSession> find(String sessionId);

    void save(ChatSession session);
}
