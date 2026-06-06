package com.taskmind.relay.projection;

import com.taskmind.events.DomainEvent;
import com.taskmind.events.EventTypes;
import java.sql.Date;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DailyMetricsProjector {
    private final JdbcTemplate jdbcTemplate;

    public DailyMetricsProjector(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void project(DomainEvent event) {
        Date day = Date.valueOf(event.occurredAt().atZone(ZoneOffset.UTC).toLocalDate());
        int tasksCreated = EventTypes.TASK_CREATED.equals(event.eventType()) ? 1 : 0;
        int tasksCompleted = isCompletion(event) ? 1 : 0;
        int projectsCreated = EventTypes.PROJECT_CREATED.equals(event.eventType()) ? 1 : 0;
        incrementUser(event.actorUserId(), day, tasksCreated, tasksCompleted, projectsCreated);
        if (event.scope().projectId() != null) {
            int projectsUpdated = EventTypes.PROJECT_UPDATED.equals(event.eventType()) || EventTypes.PROJECT_ARCHIVED.equals(event.eventType()) ? 1 : 0;
            incrementProject(event.scope().projectId(), day, tasksCreated, tasksCompleted, projectsUpdated);
        }
    }

    private boolean isCompletion(DomainEvent event) {
        return EventTypes.TASK_COMPLETED.equals(event.eventType())
                || (EventTypes.TASK_STATUS_CHANGED.equals(event.eventType()) && "DONE".equals(event.payload().path("status").asText()));
    }

    private void incrementUser(UUID userId, Date day, int tasksCreated, int tasksCompleted, int projectsCreated) {
        try {
            jdbcTemplate.update(
                    "insert into analytics.user_daily_metrics (user_id, metric_date, tasks_created, tasks_completed, projects_created, events_ingested, updated_at) values (?, ?, ?, ?, ?, 1, current_timestamp)",
                    userId, day, tasksCreated, tasksCompleted, projectsCreated);
        } catch (DuplicateKeyException ex) {
            jdbcTemplate.update(
                    "update analytics.user_daily_metrics set tasks_created=tasks_created+?, tasks_completed=tasks_completed+?, projects_created=projects_created+?, events_ingested=events_ingested+1, updated_at=current_timestamp where user_id=? and metric_date=?",
                    tasksCreated, tasksCompleted, projectsCreated, userId, day);
        }
    }

    private void incrementProject(UUID projectId, Date day, int tasksCreated, int tasksCompleted, int projectsUpdated) {
        try {
            jdbcTemplate.update(
                    "insert into analytics.project_daily_metrics (project_id, metric_date, tasks_created, tasks_completed, projects_updated, events_ingested, updated_at) values (?, ?, ?, ?, ?, 1, current_timestamp)",
                    projectId, day, tasksCreated, tasksCompleted, projectsUpdated);
        } catch (DuplicateKeyException ex) {
            jdbcTemplate.update(
                    "update analytics.project_daily_metrics set tasks_created=tasks_created+?, tasks_completed=tasks_completed+?, projects_updated=projects_updated+?, events_ingested=events_ingested+1, updated_at=current_timestamp where project_id=? and metric_date=?",
                    tasksCreated, tasksCompleted, projectsUpdated, projectId, day);
        }
    }
}
