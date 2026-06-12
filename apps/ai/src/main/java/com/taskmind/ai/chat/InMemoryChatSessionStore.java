package com.taskmind.ai.chat;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryChatSessionStore implements ChatSessionStore {
    private final ConcurrentMap<String, ChatSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Optional<ChatSession> find(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public void save(ChatSession session) {
        sessions.put(session.sessionId(), session);
    }
}
