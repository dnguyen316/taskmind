package com.taskmind.relay.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.events.DomainEvent;
import com.taskmind.events.DomainEventMapper;
import com.taskmind.events.EventTypes;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RelayStreamProjectionIngestTest {
    @Autowired IngestApplicationService ingest;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        RelayTestSchema.create(jdbcTemplate);
    }

    @Test
    void eventIngestDeduplicatesAndUpdatesProjectionMetrics() {
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        ObjectMapper objectMapper = new ObjectMapper();
        DomainEvent event =
                new DomainEvent(
                        UUID.randomUUID(),
                        1,
                        EventTypes.TASK_CREATED,
                        Instant.now(),
                        userId,
                        new DomainEvent.Scope("default", userId, projectId),
                        new DomainEvent.EntityRef("task", taskId),
                        objectMapper.createObjectNode().put("title", "Projection").put("status", "TODO"),
                        objectMapper.createObjectNode());
        String raw = new DomainEventMapper().toJson(event);

        assertTrue(ingest.ingest(raw));
        assertFalse(ingest.ingest(raw));

        Integer eventCount = jdbcTemplate.queryForObject("select count(*) from analytics.event_store", Integer.class);
        Integer taskCount = jdbcTemplate.queryForObject("select count(*) from analytics.task_projection where task_id=?", Integer.class, taskId);
        Integer metricCount = jdbcTemplate.queryForObject("select events_ingested from analytics.user_daily_metrics where user_id=?", Integer.class, userId);
        assertEquals(1, eventCount);
        assertEquals(1, taskCount);
        assertEquals(1, metricCount);
    }

    @Test
    void aiFunnelEventsUpdateDailyFunnelProjection() {
        UUID userId = UUID.randomUUID();
        ObjectMapper objectMapper = new ObjectMapper();
        DomainEvent captureEvent =
                new DomainEvent(
                        UUID.randomUUID(),
                        1,
                        EventTypes.AI_CAPTURE_SUBMITTED,
                        Instant.now(),
                        userId,
                        new DomainEvent.Scope("default", userId, null),
                        new DomainEvent.EntityRef("ai", UUID.randomUUID()),
                        objectMapper.createObjectNode().put("length", 18),
                        objectMapper.createObjectNode());
        DomainEvent acceptedEvent =
                new DomainEvent(
                        UUID.randomUUID(),
                        1,
                        EventTypes.AI_SUGGESTION_ACCEPTED,
                        captureEvent.occurredAt(),
                        userId,
                        new DomainEvent.Scope("default", userId, null),
                        new DomainEvent.EntityRef("ai", UUID.randomUUID()),
                        objectMapper.createObjectNode().put("suggestionType", "task"),
                        objectMapper.createObjectNode());

        assertTrue(ingest.ingest(new DomainEventMapper().toJson(captureEvent)));
        assertTrue(ingest.ingest(new DomainEventMapper().toJson(acceptedEvent)));

        Map<String, Object> row =
                jdbcTemplate.queryForMap(
                        "select captures_submitted, suggestions_accepted, suggestions_rejected, events_ingested from analytics.ai_funnel_daily_metrics where user_id=?",
                        userId);
        assertEquals(1, ((Number) row.get("CAPTURES_SUBMITTED")).intValue());
        assertEquals(1, ((Number) row.get("SUGGESTIONS_ACCEPTED")).intValue());
        assertEquals(0, ((Number) row.get("SUGGESTIONS_REJECTED")).intValue());
        assertEquals(2, ((Number) row.get("EVENTS_INGESTED")).intValue());
    }

    @Test
    void invalidEventLandsInDeadLetterQueue() {
        assertFalse(ingest.ingest("{bad-json"));
        Integer dlqCount = jdbcTemplate.queryForObject("select count(*) from analytics.relay_dlq", Integer.class);
        assertEquals(1, dlqCount);
    }
}
