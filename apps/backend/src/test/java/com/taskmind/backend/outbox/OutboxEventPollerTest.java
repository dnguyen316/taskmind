package com.taskmind.backend.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.outbox.application.OutboxEventWriter;
import com.taskmind.backend.outbox.application.OutboxPipelineMetrics;
import com.taskmind.backend.outbox.application.OutboxPollerJob;
import com.taskmind.backend.outbox.infrastructure.OutboxEventJpaRepository;
import com.taskmind.events.DomainEvent;
import com.taskmind.events.EventTypes;
import com.taskmind.events.transport.EventTransport;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

    @Test
    void simultaneousPollersDoNotPublishTheSameOutboxEvent() throws Exception {
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
                                .put("title", "Concurrent outbox")
                                .put("status", "TODO"),
                        objectMapper.createObjectNode());
        writer.append(event);

        CountingTransport transport = new CountingTransport();
        OutboxPollerJob first =
                new OutboxPollerJob(repository, transport, new OutboxPipelineMetrics(), 10, 100);
        OutboxPollerJob second =
                new OutboxPollerJob(repository, transport, new OutboxPipelineMetrics(), 10, 100);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstRun = executor.submit(() -> publishAfter(start, first));
            Future<?> secondRun = executor.submit(() -> publishAfter(start, second));

            start.countDown();

            firstRun.get(10, TimeUnit.SECONDS);
            secondRun.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, transport.published);
        assertEquals(Set.of(event.eventId()), transport.publishedEventIds);
        assertEquals(0, repository.unpublishedCount());
    }

    private static void publishAfter(CountDownLatch start, OutboxPollerJob poller) {
        try {
            assertTrue(start.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssertionError(ex);
        }
        poller.publishPending();
    }

    private static class CountingTransport implements EventTransport {
        volatile int published;
        Set<UUID> publishedEventIds = ConcurrentHashMap.newKeySet();

        @Override
        public String publish(String streamKey, DomainEvent event) {
            published++;
            publishedEventIds.add(event.eventId());
            return "1-0";
        }

        @Override
        public long streamLength(String streamKey) {
            return 0;
        }
    }
}
