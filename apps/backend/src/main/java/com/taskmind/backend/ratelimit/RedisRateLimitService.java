package com.taskmind.backend.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisRateLimitService implements RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<>();

    public RedisRateLimitService(StringRedisTemplate redisTemplate) {
        this(redisTemplate, Clock.systemUTC());
    }

    RedisRateLimitService(StringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    @Override
    public RateLimitDecision consume(String bucketKey, RateLimitProperties.Bucket bucket) {
        try {
            Long count = increment(bucketKey);
            if (count != null && count == 1L) {
                expire(bucketKey, bucket.refillPeriod());
            }
            long used = count == null ? 1L : count;
            if (used > bucket.capacity()) {
                return RateLimitDecision.rejected(bucket.capacity(), retryAfterSeconds(bucket));
            }
            return RateLimitDecision.allowed(bucket.capacity(), bucket.capacity() - used);
        } catch (RuntimeException ex) {
            return consumeLocally(bucketKey, bucket);
        }
    }

    protected Long increment(String bucketKey) {
        return redisTemplate.opsForValue().increment(bucketKey);
    }

    protected void expire(String bucketKey, Duration duration) {
        redisTemplate.expire(bucketKey, duration);
    }

    private RateLimitDecision consumeLocally(String bucketKey, RateLimitProperties.Bucket bucket) {
        long now = clock.millis();
        LocalCounter counter =
                localCounters.compute(
                        bucketKey,
                        (key, existing) -> {
                            if (existing == null || existing.expiresAtMillis <= now) {
                                return new LocalCounter(1, now + bucket.refillPeriod().toMillis());
                            }
                            existing.count++;
                            return existing;
                        });
        if (counter.count > bucket.capacity()) {
            return RateLimitDecision.rejected(
                    bucket.capacity(), secondsUntil(counter.expiresAtMillis, now));
        }
        return RateLimitDecision.allowed(bucket.capacity(), bucket.capacity() - counter.count);
    }

    private long retryAfterSeconds(RateLimitProperties.Bucket bucket) {
        return Math.max(1, bucket.refillPeriod().toSeconds());
    }

    private long secondsUntil(long expiresAtMillis, long now) {
        return Math.max(1, Duration.ofMillis(expiresAtMillis - now).toSeconds());
    }

    private static final class LocalCounter {
        private long count;
        private final long expiresAtMillis;

        private LocalCounter(long count, long expiresAtMillis) {
            this.count = count;
            this.expiresAtMillis = expiresAtMillis;
        }
    }
}
