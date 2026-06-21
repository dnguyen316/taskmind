package com.taskmind.relay.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.events.DomainEvent;
import com.taskmind.events.EventTypes;
import com.taskmind.relay.search.ActivityEventDocument;
import com.taskmind.relay.search.ActivityEventSearchRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ElasticsearchIndexerTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void indexesSupportedRecommendationEntityEvents() {
        CapturingRepository repository = new CapturingRepository();
        ElasticsearchIndexer indexer =
                new ElasticsearchIndexer(Optional.of(repository), ElasticsearchIndexer.DEFAULT_RECOMMENDATION_ENTITY_TYPES);

        indexer.index(event(EventTypes.TASK_CREATED, "task", mapper.createObjectNode().put("title", "Search me")));
        indexer.index(event(EventTypes.PROJECT_CREATED, "project", mapper.createObjectNode().put("name", "Roadmap")));
        indexer.index(event("attachment.uploaded", "attachment", mapper.createObjectNode().put("fileName", "brief.pdf")));
        indexer.index(event("spec.created", "spec", mapper.createObjectNode().put("title", "Import spec")));

        assertEquals(4, repository.count);
        assertEquals("Import spec", repository.last.title());
    }

    @Test
    void skipsEventsOutsideConfiguredRecommendationEntityAllowlist() {
        CapturingRepository repository = new CapturingRepository();
        ElasticsearchIndexer indexer = new ElasticsearchIndexer(Optional.of(repository), List.of("task", "project"));

        indexer.index(event(EventTypes.TASK_CREATED, "task", mapper.createObjectNode().put("title", "Search me")));
        indexer.index(event("billing.invoice.created", "invoice", mapper.createObjectNode().put("title", "Excluded")));

        assertEquals(1, repository.count);
        assertEquals("task", repository.last.entityType());
    }

    private DomainEvent event(String eventType, String entityType, com.fasterxml.jackson.databind.JsonNode payload) {
        UUID userId = UUID.randomUUID();
        return new DomainEvent(
                UUID.randomUUID(),
                1,
                eventType,
                Instant.now(),
                userId,
                new DomainEvent.Scope("default", userId, UUID.randomUUID()),
                new DomainEvent.EntityRef(entityType, UUID.randomUUID()),
                payload,
                mapper.createObjectNode());
    }

    static class CapturingRepository implements ActivityEventSearchRepository {
        int count;
        ActivityEventDocument last;

        @Override
        public void index(ActivityEventDocument document) {
            count++;
            last = document;
        }
    }
}
