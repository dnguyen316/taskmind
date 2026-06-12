package com.taskmind.ai.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "taskmind.ai.chat.redis-enabled", havingValue = "true")
public class RedisChatSessionStore implements ChatSessionStore {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisChatSessionStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${taskmind.ai.chat.session-ttl:PT2H}") Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public Optional<ChatSession> find(String sessionId) {
        try {
            String value = redisTemplate.opsForValue().get(key(sessionId));
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, ChatSession.class));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    @Override
    public void save(ChatSession session) {
        try {
            redisTemplate
                    .opsForValue()
                    .set(key(session.sessionId()), objectMapper.writeValueAsString(session), ttl);
        } catch (Exception ignored) {
            // Test-safe fallback: a transient Redis failure must not fail the Nova chat turn.
        }
    }

    private String key(String sessionId) {
        return "nova:chat:" + sessionId;
    }
}
