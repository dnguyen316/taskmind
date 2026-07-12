package com.taskmind.relay.ingest;

import jakarta.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "taskmind.relay.enabled", havingValue = "true", matchIfMissing = true)
public class StreamEventConsumerJob {
    private static final Logger log = LoggerFactory.getLogger(StreamEventConsumerJob.class);
    private static final String EVENT_FIELD = "event";

    private final StringRedisTemplate redisTemplate;
    private final IngestApplicationService ingestApplicationService;
    private final String streamKey;
    private final String groupName;
    private final String consumerName;
    private final int batchSize;
    private final int maxDeliveryAttempts;
    private final Duration pendingIdleTimeout;

    public StreamEventConsumerJob(
            StringRedisTemplate redisTemplate,
            IngestApplicationService ingestApplicationService,
            @Value("${taskmind.relay.stream-key:taskmind.events}") String streamKey,
            @Value("${taskmind.relay.consumer-group:taskmind-relay}") String groupName,
            @Value("${taskmind.relay.consumer-name:#{null}}") String consumerName,
            @Value("${taskmind.relay.batch-size:100}") int batchSize,
            @Value("${taskmind.relay.max-delivery-attempts:5}") int maxDeliveryAttempts,
            @Value("${taskmind.relay.pending-idle-timeout-ms:30000}") long pendingIdleTimeoutMs) {
        this.redisTemplate = redisTemplate;
        this.ingestApplicationService = ingestApplicationService;
        this.streamKey = streamKey;
        this.groupName = groupName;
        this.consumerName = consumerName == null || consumerName.isBlank() ? defaultConsumerName() : consumerName;
        this.batchSize = Math.max(1, batchSize);
        this.maxDeliveryAttempts = Math.max(1, maxDeliveryAttempts);
        this.pendingIdleTimeout = Duration.ofMillis(Math.max(1, pendingIdleTimeoutMs));
    }

    @PostConstruct
    public void ensureConsumerGroup() {
        StreamOperations<String, String, String> streams = redisTemplate.opsForStream();
        try {
            streams.createGroup(streamKey, ReadOffset.latest(), groupName);
        } catch (DataAccessException ex) {
            if (isGroupAlreadyExists(ex)) {
                return;
            }
            try {
                RecordId seedRecord = streams.add(streamKey, Map.of("_bootstrap", "true"));
                streams.createGroup(streamKey, ReadOffset.latest(), groupName);
                if (seedRecord != null) {
                    streams.delete(streamKey, seedRecord);
                }
            } catch (DataAccessException nested) {
                if (!isGroupAlreadyExists(nested)) {
                    throw nested;
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${taskmind.relay.poll-interval-ms:1000}")
    public void consumeAvailable() {
        StreamOperations<String, String, String> streams = redisTemplate.opsForStream();
        processPending(streams);
        processNewRecords(streams);
    }

    private void processPending(StreamOperations<String, String, String> streams) {
        PendingMessages pending = streams.pending(streamKey, groupName, Range.unbounded(), batchSize);
        if (pending == null || pending.isEmpty()) {
            return;
        }
        for (PendingMessage message : pending) {
            if (message.getTotalDeliveryCount() >= maxDeliveryAttempts) {
                deadLetterPending(streams, message);
                continue;
            }
            if (message.getElapsedTimeSinceLastDelivery().compareTo(pendingIdleTimeout) < 0) {
                continue;
            }
            List<MapRecord<String, String, String>> claimed = streams.claim(
                    streamKey, groupName, consumerName, pendingIdleTimeout, message.getId());
            if (claimed != null) {
                claimed.forEach(record -> processRecord(streams, record));
            }
        }
    }

    private void processNewRecords(StreamOperations<String, String, String> streams) {
        List<MapRecord<String, String, String>> records = streams.read(
                Consumer.from(groupName, consumerName),
                StreamReadOptions.empty().count(batchSize).block(pollTimeout()),
                StreamOffset.create(streamKey, ReadOffset.lastConsumed()));
        if (records == null) {
            return;
        }
        records.forEach(record -> processRecord(streams, record));
    }

    private void processRecord(StreamOperations<String, String, String> streams, MapRecord<String, String, String> record) {
        String event = record.getValue().get(EVENT_FIELD);
        if (event == null) {
            deadLetter(streams, record, "missing event field");
            acknowledge(streams, record);
            return;
        }
        if (ingestApplicationService.ingest(event)) {
            acknowledge(streams, record);
        }
    }

    private void deadLetterPending(StreamOperations<String, String, String> streams, PendingMessage message) {
        List<MapRecord<String, String, String>> claimed = streams.claim(
                streamKey, groupName, consumerName, Duration.ZERO, message.getId());
        if (claimed == null || claimed.isEmpty()) {
            streams.acknowledge(streamKey, groupName, message.getId());
            return;
        }
        for (MapRecord<String, String, String> record : claimed) {
            deadLetter(streams, record, "delivery attempts exceeded");
            acknowledge(streams, record);
        }
    }

    private void deadLetter(StreamOperations<String, String, String> streams, MapRecord<String, String, String> record, String reason) {
        streams.add(streamKey + ".dlq", Map.of(
                "sourceStream", streamKey,
                "sourceId", record.getId().getValue(),
                "reason", reason,
                "event", record.getValue().getOrDefault(EVENT_FIELD, "")));
        log.warn("Moved Redis stream record {} to dead-letter stream: {}", record.getId(), reason);
    }

    private void acknowledge(StreamOperations<String, String, String> streams, MapRecord<String, String, String> record) {
        streams.acknowledge(streamKey, groupName, record.getId());
    }

    public StreamOffset<String> streamOffset() {
        return StreamOffset.create(streamKey, ReadOffset.lastConsumed());
    }

    public Duration pollTimeout() {
        return Duration.ofMillis(1000);
    }

    private static boolean isGroupAlreadyExists(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("BUSYGROUP")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String defaultConsumerName() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (UnknownHostException ex) {
            return "relay-" + UUID.randomUUID();
        }
    }
}
