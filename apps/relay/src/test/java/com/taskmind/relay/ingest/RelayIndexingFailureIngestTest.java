package com.taskmind.relay.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.events.DomainEvent;
import com.taskmind.events.DomainEventMapper;
import com.taskmind.events.EventTypes;
import com.taskmind.relay.search.ActivityEventDocument;
import com.taskmind.relay.search.ActivityEventSearchRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RelayIndexingFailureIngestTest {
    @Autowired IngestApplicationService ingest;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired FailingOnceSearchRepository searchRepository;

    @BeforeEach
    void setUp() {
        RelayTestSchema.create(jdbcTemplate);
        searchRepository.reset();
    }

    @Test
    void indexingFailureRollsBackEventStoreSoRetryCanIndexSameEvent() {
        DomainEvent event = taskCreatedEvent();
        String raw = new DomainEventMapper().toJson(event);

        searchRepository.failNextIndex();

        assertFalse(ingest.ingest(raw));
        assertEquals(0, count("analytics.event_store"));
        assertEquals(0, count("analytics.task_projection"));
        assertEquals(1, count("analytics.relay_dlq"));
        assertEquals(0, searchRepository.indexedDocuments.size());

        assertTrue(ingest.ingest(raw));
        assertEquals(1, count("analytics.event_store"));
        assertEquals(1, count("analytics.task_projection"));
        assertEquals(1, searchRepository.indexedDocuments.size());
        assertEquals(event.eventId(), searchRepository.indexedDocuments.get(0).eventId());
    }

    private Integer count(String table) {
        return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
    }

    private static DomainEvent taskCreatedEvent() {
        ObjectMapper objectMapper = new ObjectMapper();
        UUID userId = UUID.randomUUID();
        return new DomainEvent(
                UUID.randomUUID(),
                1,
                EventTypes.TASK_CREATED,
                Instant.now(),
                userId,
                new DomainEvent.Scope("default", userId, UUID.randomUUID()),
                new DomainEvent.EntityRef("task", UUID.randomUUID()),
                objectMapper.createObjectNode().put("title", "Search retry").put("status", "TODO"),
                objectMapper.createObjectNode());
    }

    @TestConfiguration
    static class TestSearchConfig {
        @Bean
        FailingOnceSearchRepository failingOnceSearchRepository() {
            return new FailingOnceSearchRepository();
        }
    }

    static class FailingOnceSearchRepository implements ActivityEventSearchRepository {
        private final List<ActivityEventDocument> indexedDocuments = new ArrayList<>();
        private boolean failNext;

        void failNextIndex() {
            failNext = true;
        }

        void reset() {
            failNext = false;
            indexedDocuments.clear();
        }

        @Override
        public void index(ActivityEventDocument document) {
            if (failNext) {
                failNext = false;
                throw new IllegalStateException("OpenSearch unavailable");
            }
            indexedDocuments.add(document);
        }
    }
}
