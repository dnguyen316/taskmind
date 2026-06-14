package com.taskmind.relay.projection;

import com.taskmind.events.DomainEvent;
import com.taskmind.events.EventTypes;
import java.sql.Date;
import java.time.ZoneOffset;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AiFunnelProjectionHandler {
    private static final Set<String> AI_FUNNEL_EVENTS =
            Set.of(
                    EventTypes.AI_CAPTURE_SUBMITTED,
                    EventTypes.AI_SUGGESTION_ACCEPTED,
                    EventTypes.AI_SUGGESTION_REJECTED);

    private final JdbcTemplate jdbcTemplate;

    public AiFunnelProjectionHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void project(DomainEvent event) {
        if (!AI_FUNNEL_EVENTS.contains(event.eventType())) {
            return;
        }
        Date day = Date.valueOf(event.occurredAt().atZone(ZoneOffset.UTC).toLocalDate());
        int captures = EventTypes.AI_CAPTURE_SUBMITTED.equals(event.eventType()) ? 1 : 0;
        int accepted = EventTypes.AI_SUGGESTION_ACCEPTED.equals(event.eventType()) ? 1 : 0;
        int rejected = EventTypes.AI_SUGGESTION_REJECTED.equals(event.eventType()) ? 1 : 0;
        try {
            jdbcTemplate.update(
                    "insert into analytics.ai_funnel_daily_metrics (user_id, metric_date, captures_submitted, suggestions_accepted, suggestions_rejected, events_ingested, updated_at) values (?, ?, ?, ?, ?, 1, current_timestamp)",
                    event.scope().userId(), day, captures, accepted, rejected);
        } catch (DuplicateKeyException ex) {
            jdbcTemplate.update(
                    "update analytics.ai_funnel_daily_metrics set captures_submitted=captures_submitted+?, suggestions_accepted=suggestions_accepted+?, suggestions_rejected=suggestions_rejected+?, events_ingested=events_ingested+1, updated_at=current_timestamp where user_id=? and metric_date=?",
                    captures, accepted, rejected, event.scope().userId(), day);
        }
    }
}
