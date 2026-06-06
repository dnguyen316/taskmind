package com.taskmind.relay.projection;

import com.taskmind.events.DomainEvent;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProjectProjectionHandler {
    private final JdbcTemplate jdbcTemplate;

    public ProjectProjectionHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void project(DomainEvent event) {
        if (!event.entity().type().equals("project")) {
            return;
        }
        String name = event.payload().path("name").asText("");
        String key = event.payload().path("key").asText("");
        boolean archived = event.payload().path("archived").asBoolean(false);
        try {
            jdbcTemplate.update(
                    "insert into analytics.project_projection (project_id, owner_user_id, name, project_key, archived, updated_at) values (?, ?, ?, ?, ?, ?)",
                    event.entity().id(), event.scope().userId(), name, key, archived, event.occurredAt());
        } catch (DuplicateKeyException ex) {
            jdbcTemplate.update(
                    "update analytics.project_projection set owner_user_id=?, name=?, project_key=?, archived=?, updated_at=? where project_id=?",
                    event.scope().userId(), name, key, archived, event.occurredAt(), event.entity().id());
        }
    }
}
