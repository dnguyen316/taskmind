package com.taskmind.backend.outbox.application;

import com.taskmind.backend.outbox.infrastructure.OutboxEventJpaEntity;
import com.taskmind.backend.outbox.infrastructure.OutboxEventJpaRepository;
import com.taskmind.events.DomainEvent;
import com.taskmind.events.DomainEventMapper;
import com.taskmind.events.transport.EventTransport;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        name = "taskmind.outbox.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class OutboxPollerJob {
    private final OutboxEventJpaRepository repository;
    private final EventTransport transport;
    private final DomainEventMapper mapper = new DomainEventMapper();
    private final OutboxPipelineMetrics metrics;
    private final int batchSize;
    private final long backpressureLength;
    private final String pollerId = UUID.randomUUID().toString();

    public OutboxPollerJob(
            OutboxEventJpaRepository repository,
            EventTransport transport,
            OutboxPipelineMetrics metrics,
            @Value("${taskmind.outbox.batch-size:50}") int batchSize,
            @Value("${taskmind.outbox.backpressure-stream-length:10000}") long backpressureLength) {
        this.repository = repository;
        this.transport = transport;
        this.metrics = metrics;
        this.batchSize = batchSize;
        this.backpressureLength = backpressureLength;
    }

    @Scheduled(fixedDelayString = "${taskmind.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        for (OutboxEventJpaEntity row : repository.claimPending(pollerId, Instant.now(), batchSize)) {
            if (transport.streamLength(row.streamKey()) >= backpressureLength) {
                return;
            }
            try {
                DomainEvent event = mapper.fromJson(row.payload());
                transport.publish(row.streamKey(), event);
                row.markPublished(Instant.now());
                repository.save(row);
                metrics.recordPublished();
            } catch (RuntimeException ex) {
                row.markPublishFailed(ex.getMessage(), Instant.now());
                repository.save(row);
                metrics.recordFailed();
            }
        }
    }
}
