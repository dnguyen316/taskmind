package com.taskmind.relay.sink;

import com.taskmind.events.DomainEvent;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventStoreWriter {
    private final JdbcTemplate jdbcTemplate;

    public EventStoreWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean writeIfNew(DomainEvent event) {
        try {
            jdbcTemplate.update(
                    "insert into analytics.event_store (event_id, event_type, actor_user_id, entity_type, entity_id, project_id, occurred_at, payload, context, received_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp)",
                    event.eventId(),
                    event.eventType(),
                    event.actorUserId(),
                    event.entity().type(),
                    event.entity().id(),
                    event.scope().projectId(),
                    event.occurredAt(),
                    event.payload().toString(),
                    event.context().toString());
            return true;
        } catch (DuplicateKeyException ex) {
            return false;
        }
    }
}
