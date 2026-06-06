package com.taskmind.backend.outbox.infrastructure;

import com.taskmind.events.DomainEvent;
import com.taskmind.events.DomainEventMapper;
import com.taskmind.events.transport.EventTransport;
import java.util.Map;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisStreamEventTransport implements EventTransport {
    private final StringRedisTemplate redisTemplate;
    private final DomainEventMapper mapper = new DomainEventMapper();

    public RedisStreamEventTransport(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String publish(String streamKey, DomainEvent event) {
        MapRecord<String, String, String> record =
                MapRecord.create(streamKey, Map.of("event", mapper.toJson(event)));
        return redisTemplate.opsForStream().add(record).getValue();
    }

    @Override
    public long streamLength(String streamKey) {
        Long size = redisTemplate.opsForStream().size(streamKey);
        return size == null ? 0 : size;
    }
}
