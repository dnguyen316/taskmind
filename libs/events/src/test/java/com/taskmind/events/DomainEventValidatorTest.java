package com.taskmind.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainEventValidatorTest {
    @Test
    void validatesKnownEventType() {
        UUID userId = UUID.randomUUID();
        DomainEvent event =
                new DomainEvent(
                        UUID.randomUUID(),
                        1,
                        EventTypes.TASK_CREATED,
                        Instant.now(),
                        userId,
                        new DomainEvent.Scope("default", userId, UUID.randomUUID()),
                        new DomainEvent.EntityRef("task", UUID.randomUUID()),
                        new ObjectMapper().createObjectNode(),
                        new ObjectMapper().createObjectNode());
        assertDoesNotThrow(() -> new DomainEventValidator().validate(event));
    }
}
