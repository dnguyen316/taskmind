package com.taskmind.relay.sink;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.events.DomainEvent;
import com.taskmind.events.EventTypes;
import com.taskmind.relay.search.ActivityEventDocument;
import com.taskmind.relay.search.ActivityEventSearchRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ElasticsearchIndexerTest {
    @Test
    void indexesTaskEventsOnly() {
        CapturingRepository repository = new CapturingRepository();
        ElasticsearchIndexer indexer = new ElasticsearchIndexer(Optional.of(repository));
        ObjectMapper mapper = new ObjectMapper();
        UUID userId = UUID.randomUUID();
        DomainEvent taskEvent = new DomainEvent(UUID.randomUUID(), 1, EventTypes.TASK_CREATED, Instant.now(), userId, new DomainEvent.Scope("default", userId, UUID.randomUUID()), new DomainEvent.EntityRef("task", UUID.randomUUID()), mapper.createObjectNode().put("title", "Search me"), mapper.createObjectNode());
        DomainEvent projectEvent = new DomainEvent(UUID.randomUUID(), 1, EventTypes.PROJECT_CREATED, Instant.now(), userId, new DomainEvent.Scope("default", userId, UUID.randomUUID()), new DomainEvent.EntityRef("project", UUID.randomUUID()), mapper.createObjectNode(), mapper.createObjectNode());

        indexer.index(taskEvent);
        indexer.index(projectEvent);

        assertEquals(1, repository.count);
        assertEquals("Search me", repository.last.title());
    }

    static class CapturingRepository implements ActivityEventSearchRepository {
        int count;
        ActivityEventDocument last;
        @Override public void index(ActivityEventDocument document) { count++; last = document; }
    }
}
