package com.taskmind.relay.ingest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "taskmind.relay.enabled", havingValue = "true", matchIfMissing = true)
public class StreamEventConsumerJob {
    private final StringRedisTemplate redisTemplate;
    private final IngestApplicationService ingestApplicationService;
    private final String streamKey;

    public StreamEventConsumerJob(StringRedisTemplate redisTemplate, IngestApplicationService ingestApplicationService, @Value("${taskmind.relay.stream-key:taskmind.events}") String streamKey) {
        this.redisTemplate = redisTemplate;
        this.ingestApplicationService = ingestApplicationService;
        this.streamKey = streamKey;
    }

    @Scheduled(fixedDelayString = "${taskmind.relay.poll-interval-ms:1000}")
    public void consumeAvailable() {
        StreamOperations<String, String, String> streams = redisTemplate.opsForStream();
        List<MapRecord<String, String, String>> records = streams.range(streamKey, Range.unbounded());
        if (records == null) {
            return;
        }
        for (MapRecord<String, String, String> record : records) {
            Map<String, String> values = record.getValue();
            String event = values.get("event");
            if (event != null && ingestApplicationService.ingest(event)) {
                redisTemplate.opsForStream().delete(streamKey, record.getId());
            }
        }
    }

    public StreamOffset<String> streamOffset() {
        return StreamOffset.create(streamKey, ReadOffset.from("0-0"));
    }

    public Duration pollTimeout() {
        return Duration.ofMillis(1000);
    }
}
