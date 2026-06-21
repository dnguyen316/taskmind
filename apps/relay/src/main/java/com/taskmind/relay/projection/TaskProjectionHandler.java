package com.taskmind.relay.projection;

import com.taskmind.events.DomainEvent;
import com.taskmind.events.EventTypes;
import com.taskmind.relay.jdbc.RelayJdbcParameters;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TaskProjectionHandler {
    private final JdbcTemplate jdbcTemplate;

    public TaskProjectionHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void project(DomainEvent event) {
        if (!event.entity().type().equals("task")) {
            return;
        }
        UUID taskId = event.entity().id();
        String title = event.payload().path("title").asText("");
        String status = event.payload().path("status").asText("TODO");
        try {
            jdbcTemplate.update(
                    "insert into analytics.task_projection (task_id, user_id, project_id, title, status, updated_at) values (?, ?, ?, ?, ?, ?)",
                    taskId,
                    event.scope().userId(),
                    event.scope().projectId(),
                    title,
                    status,
                    RelayJdbcParameters.timestamp(event.occurredAt()));
        } catch (DuplicateKeyException ex) {
            jdbcTemplate.update(
                    "update analytics.task_projection set user_id=?, project_id=?, title=?, status=?, updated_at=? where task_id=?",
                    event.scope().userId(),
                    event.scope().projectId(),
                    title,
                    status,
                    RelayJdbcParameters.timestamp(event.occurredAt()),
                    taskId);
        }
        if (EventTypes.TASK_ARCHIVED.equals(event.eventType()) || EventTypes.TASK_DELETED.equals(event.eventType())) {
            jdbcTemplate.update("delete from analytics.task_projection where task_id=?", taskId);
        }
    }
}
