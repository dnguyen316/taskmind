package com.taskmind.backend.outbox.application;

import com.taskmind.backend.outbox.infrastructure.OutboxEventJpaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OutboxPipelineMetrics {
    public static final String PUBLISHED_COUNTER = "taskmind.outbox.events.published";
    public static final String FAILED_COUNTER = "taskmind.outbox.events.failed";
    public static final String LAG_GAUGE = "taskmind.outbox.lag.events";

    private final Counter published;
    private final Counter failed;

    public OutboxPipelineMetrics(MeterRegistry meterRegistry, OutboxEventJpaRepository repository) {
        this.published =
                Counter.builder(PUBLISHED_COUNTER)
                        .description("Outbox events successfully published to the relay stream")
                        .register(meterRegistry);
        this.failed =
                Counter.builder(FAILED_COUNTER)
                        .description("Outbox events that failed during publish attempts")
                        .register(meterRegistry);
        Gauge.builder(LAG_GAUGE, repository, OutboxEventJpaRepository::pendingCount)
                .description("Outbox events waiting to be claimed and published")
                .strongReference(true)
                .register(meterRegistry);
    }

    public void recordPublished() {
        published.increment();
    }

    public void recordFailed() {
        failed.increment();
    }

    public long publishedCount() {
        return Math.round(published.count());
    }

    public long failedCount() {
        return Math.round(failed.count());
    }
}
