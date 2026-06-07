package com.taskmind.relay.dlq;

import com.taskmind.events.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RelayDeadLetterWriter {
    private final JdbcTemplate jdbcTemplate;

    public RelayDeadLetterWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(DomainEvent event, String rawPayload, Exception error) {
        UUID eventId = event == null ? null : event.eventId();
        String eventType = event == null ? null : event.eventType();
        jdbcTemplate.update(
                "insert into analytics.relay_dlq (id, event_id, event_type, payload, error_message, failed_at) values (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), eventId, eventType, rawPayload, error.getMessage(), Instant.now());
    }
}
