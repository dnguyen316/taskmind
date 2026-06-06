package com.taskmind.relay.export;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RelayExportService {
    private final JdbcTemplate jdbcTemplate;

    public RelayExportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> recentEvents(int limit) {
        return jdbcTemplate.queryForList(
                "select event_id, event_type, actor_user_id, entity_type, entity_id, occurred_at from analytics.event_store order by occurred_at desc limit ?",
                limit);
    }
}
