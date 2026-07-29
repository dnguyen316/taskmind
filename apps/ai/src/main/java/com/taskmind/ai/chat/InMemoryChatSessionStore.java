package com.taskmind.ai.chat;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "taskmind.ai.chat.redis-enabled",
        havingValue = "false",
        matchIfMissing = true)
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
