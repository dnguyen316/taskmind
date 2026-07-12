package com.taskmind.relay.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RelayPipelineMetrics {
    public static final String RESULT_INGESTED = "ingested";
    public static final String RESULT_DUPLICATE = "duplicate";
    public static final String RESULT_DEAD_LETTER = "dead_letter";
    public static final String RESULT_FAILED = "failed";

    private final MeterRegistry meterRegistry;
    private final StringRedisTemplate redisTemplate;
    private final String streamKey;
    private final String groupName;
    private final Counter ingested;
    private final Counter duplicates;
    private final Counter deadLetters;

    public RelayPipelineMetrics(
            MeterRegistry meterRegistry,
            StringRedisTemplate redisTemplate,
            @Value("${taskmind.relay.stream-key:taskmind.events}") String streamKey,
            @Value("${taskmind.relay.consumer-group:taskmind-relay}") String groupName) {
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
        this.streamKey = streamKey;
        this.groupName = groupName;
        this.ingested = Counter.builder("taskmind.relay.events.ingested")
                .description("Relay domain events successfully ingested")
                .register(meterRegistry);
        this.duplicates = Counter.builder("taskmind.relay.events.duplicates")
                .description("Relay domain events skipped because they were already ingested")
                .register(meterRegistry);
        this.deadLetters = Counter.builder("taskmind.relay.events.dead_letters")
                .description("Relay domain events written to dead-letter storage")
                .register(meterRegistry);
        Gauge.builder("taskmind.relay.stream.pending", this, RelayPipelineMetrics::pendingCount)
                .description("Redis stream pending entry count for the relay consumer group")
                .register(meterRegistry);
        Gauge.builder("taskmind.relay.stream.length", this, RelayPipelineMetrics::streamLength)
                .description("Redis stream length for relay input events")
                .register(meterRegistry);
    }

    public void recordIngested() {
        ingested.increment();
    }

    public void recordDuplicate() {
        duplicates.increment();
    }

    public void recordDeadLetter() {
        deadLetters.increment();
    }

    public String recordStreamProcessing(Callable<String> operation) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = RESULT_FAILED;
        try {
            outcome = operation.call();
            return outcome;
        } catch (Exception ex) {
            outcome = RESULT_FAILED;
            throw propagate(ex);
        } finally {
            sample.stop(Timer.builder("taskmind.relay.stream.processing.duration")
                    .description("Redis stream record processing duration")
                    .tag("result", outcome)
                    .register(meterRegistry));
        }
    }

    public long ingestedCount() {
        return Math.round(ingested.count());
    }

    public long duplicateCount() {
        return Math.round(duplicates.count());
    }

    public long deadLetterCount() {
        return Math.round(deadLetters.count());
    }

    private double pendingCount() {
        try {
            PendingMessagesSummary pending = redisTemplate.opsForStream().pending(streamKey, groupName);
            return pending == null ? 0 : pending.getTotalPendingMessages();
        } catch (RuntimeException ex) {
            return Double.NaN;
        }
    }

    private double streamLength() {
        try {
            Long size = redisTemplate.opsForStream().size(streamKey);
            return size == null ? 0 : size;
        } catch (RuntimeException ex) {
            return Double.NaN;
        }
    }

    private static RuntimeException propagate(Exception ex) {
        if (ex instanceof RuntimeException runtimeException) {
            return runtimeException;
        }
        return new IllegalStateException(ex);
    }
}
