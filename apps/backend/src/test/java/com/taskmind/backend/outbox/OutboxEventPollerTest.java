package com.taskmind.backend.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.outbox.application.OutboxEventWriter;
import com.taskmind.backend.outbox.application.OutboxPipelineMetrics;
import com.taskmind.backend.outbox.application.OutboxPollerJob;
import com.taskmind.backend.outbox.infrastructure.OutboxEventJpaRepository;
import com.taskmind.events.DomainEvent;
import com.taskmind.events.EventTypes;
import com.taskmind.events.transport.EventTransport;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OutboxEventPollerTest {
    @Autowired OutboxEventWriter writer;
    @Autowired OutboxEventJpaRepository repository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void pollerPublishesPendingOutboxEvent() {
        jdbcTemplate.update("delete from outbox_events");
        UUID userId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        ObjectMapper objectMapper = new ObjectMapper();
        DomainEvent event =
                new DomainEvent(
                        UUID.randomUUID(),
                        1,
                        EventTypes.TASK_CREATED,
                        Instant.now(),
                        userId,
                        new DomainEvent.Scope("default", userId, projectId),
                        new DomainEvent.EntityRef("task", UUID.randomUUID()),
                        objectMapper
                                .createObjectNode()
                                .put("title", "Outbox")
                                .put("status", "TODO"),
                        objectMapper.createObjectNode());
        writer.append(event);
        CountingTransport transport = new CountingTransport();
        OutboxPollerJob poller =
                new OutboxPollerJob(repository, transport, new OutboxPipelineMetrics(), 10, 100);

        poller.publishPending();

        assertEquals(1, transport.published);
        assertEquals(0, repository.unpublishedCount());
    }

    private static class CountingTransport implements EventTransport {
        int published;

        @Override
        public String publish(String streamKey, DomainEvent event) {
            published++;
            return "1-0";
        }

        @Override
        public long streamLength(String streamKey) {
            return 0;
        }
    }
}
