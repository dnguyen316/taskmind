package com.taskmind.backend.ai.application;

import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "test"})
@ConditionalOnMissingBean(AiDomainEventPublisher.class)
class NoopAiDomainEventPublisher implements AiDomainEventPublisher {
    @Override
    public void publish(UUID userId, String eventType, Map<String, Object> payload) {
        // Local/test fallback when the outbox-backed publisher bean is intentionally absent.
    }
}
