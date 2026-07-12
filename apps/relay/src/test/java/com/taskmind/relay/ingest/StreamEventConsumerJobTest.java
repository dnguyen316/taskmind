package com.taskmind.relay.ingest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

class StreamEventConsumerJobTest {
    private static final String STREAM = "taskmind.events";
    private static final String GROUP = "taskmind-relay";

    @Test
    void createsConsumerGroupAtStartup() {
        StreamOperations<String, String, String> streams = mock(StreamOperations.class);
        StringRedisTemplate redis = redis(streams);

        job(redis, "relay-1").ensureConsumerGroup();

        verify(streams).createGroup(STREAM, ReadOffset.latest(), GROUP);
    }

    @Test
    void consumerGroupReadGivesNewStreamEntryToOnlyOneConsumerBeforeRedelivery() {
        StreamOperations<String, String, String> streams = mock(StreamOperations.class);
        StringRedisTemplate redis = redis(streams);
        TestIngestApplicationService ingest = new TestIngestApplicationService(true);
        MapRecord<String, String, String> record = record("1-0", "payload-1");
        when(streams.pending(eq(STREAM), eq(GROUP), any(Range.class), eq(10L))).thenReturn(emptyPending());
        when(streams.read(eq(Consumer.from(GROUP, "relay-1")), any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenReturn(List.of(record));
        when(streams.read(eq(Consumer.from(GROUP, "relay-2")), any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenReturn(List.of());

        job(redis, ingest, "relay-1").consumeAvailable();
        job(redis, ingest, "relay-2").consumeAvailable();

        org.assertj.core.api.Assertions.assertThat(ingest.ingestedPayloads()).containsExactly("payload-1");
        verify(streams).acknowledge(STREAM, GROUP, RecordId.of("1-0"));
        verify(streams, never()).delete(eq(STREAM), any(RecordId[].class));
    }

    @Test
    void claimsStalePendingEntryForIntentionalRedelivery() {
        StreamOperations<String, String, String> streams = mock(StreamOperations.class);
        StringRedisTemplate redis = redis(streams);
        TestIngestApplicationService ingest = new TestIngestApplicationService(true);
        PendingMessage pending = new PendingMessage(RecordId.of("2-0"), Consumer.from(GROUP, "relay-1"), java.time.Duration.ofMinutes(2), 2);
        MapRecord<String, String, String> record = record("2-0", "payload-2");
        when(streams.pending(eq(STREAM), eq(GROUP), any(Range.class), eq(10L)))
                .thenReturn(new PendingMessages(GROUP, List.of(pending)));
        when(streams.claim(STREAM, GROUP, "relay-2", java.time.Duration.ofMillis(1), RecordId.of("2-0")))
                .thenReturn(List.of(record));
        when(streams.read(eq(Consumer.from(GROUP, "relay-2")), any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenReturn(List.of());

        job(redis, ingest, "relay-2", 5, 1).consumeAvailable();

        org.assertj.core.api.Assertions.assertThat(ingest.ingestedPayloads()).containsExactly("payload-2");
        verify(streams).acknowledge(STREAM, GROUP, RecordId.of("2-0"));
    }

    @Test
    void deadLettersPendingEntryAfterRepeatedFailures() {
        StreamOperations<String, String, String> streams = mock(StreamOperations.class);
        StringRedisTemplate redis = redis(streams);
        PendingMessage pending = new PendingMessage(RecordId.of("3-0"), Consumer.from(GROUP, "relay-1"), java.time.Duration.ofMinutes(2), 5);
        MapRecord<String, String, String> record = record("3-0", "payload-3");
        when(streams.pending(eq(STREAM), eq(GROUP), any(Range.class), eq(10L)))
                .thenReturn(new PendingMessages(GROUP, List.of(pending)));
        when(streams.claim(STREAM, GROUP, "relay-2", java.time.Duration.ZERO, RecordId.of("3-0")))
                .thenReturn(List.of(record));
        when(streams.read(eq(Consumer.from(GROUP, "relay-2")), any(StreamReadOptions.class), any(StreamOffset.class)))
                .thenReturn(List.of());

        job(redis, "relay-2").consumeAvailable();

        verify(streams).add(eq(STREAM + ".dlq"), any(Map.class));
        verify(streams).acknowledge(STREAM, GROUP, RecordId.of("3-0"));
    }

    private static final class TestIngestApplicationService extends IngestApplicationService {
        private final boolean result;
        private final java.util.List<String> ingestedPayloads = new java.util.ArrayList<>();

        private TestIngestApplicationService(boolean result) {
            super(null, null, null, null, null, null, null, null);
            this.result = result;
        }

        @Override
        public boolean ingest(String rawPayload) {
            ingestedPayloads.add(rawPayload);
            return result;
        }

        private java.util.List<String> ingestedPayloads() {
            return ingestedPayloads;
        }
    }

    private static StringRedisTemplate redis(StreamOperations<String, String, String> streams) {
        return new StringRedisTemplate() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public StreamOperations<String, Object, Object> opsForStream() {
                return (StreamOperations) streams;
            }
        };
    }

    private static StreamEventConsumerJob job(StringRedisTemplate redis, String consumerName) {
        return job(redis, new TestIngestApplicationService(true), consumerName);
    }

    private static StreamEventConsumerJob job(StringRedisTemplate redis, IngestApplicationService ingest, String consumerName) {
        return job(redis, ingest, consumerName, 5, 30_000);
    }

    private static StreamEventConsumerJob job(StringRedisTemplate redis, IngestApplicationService ingest, String consumerName, int maxAttempts, long pendingIdleMs) {
        return new StreamEventConsumerJob(redis, ingest, STREAM, GROUP, consumerName, 10, maxAttempts, pendingIdleMs);
    }

    private static MapRecord<String, String, String> record(String id, String payload) {
        return MapRecord.create(STREAM, Map.of("event", payload)).withId(RecordId.of(id));
    }

    private static PendingMessages emptyPending() {
        return new PendingMessages(GROUP, List.of());
    }
}
