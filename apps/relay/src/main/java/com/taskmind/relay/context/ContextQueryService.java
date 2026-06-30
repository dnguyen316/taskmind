package com.taskmind.relay.context;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ContextQueryService {
    private final JdbcTemplate jdbcTemplate;

    public ContextQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> userTasks(UUID userId) {
        return jdbcTemplate.queryForList(
                "select task_id, project_id, title, status, task_type_key, updated_at from analytics.task_projection where user_id=? order by updated_at desc limit 50",
                userId);
    }

    public List<Map<String, Object>> projectTasks(UUID projectId) {
        return jdbcTemplate.queryForList(
                "select task_id, user_id, title, status, task_type_key, updated_at from analytics.task_projection where project_id=? order by updated_at desc limit 50",
                projectId);
    }

    public List<Map<String, Object>> projectMetrics(UUID projectId) {
        return jdbcTemplate.queryForList(
                "select metric_date, tasks_created, tasks_completed, projects_updated, events_ingested from analytics.project_daily_metrics where project_id=? order by metric_date desc limit 30",
                projectId);
    }
}
