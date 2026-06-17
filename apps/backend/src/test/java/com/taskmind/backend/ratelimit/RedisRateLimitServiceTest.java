package com.taskmind.backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;

class RedisRateLimitServiceTest {

    @Test
    void fallsBackToLocalBucketWhenRedisIsUnavailable() {
        RedisRateLimitService service = new RedisFailureRateLimitService();
        RateLimitProperties.Bucket bucket = new RateLimitProperties.Bucket(1, Duration.ofMinutes(1));

        RateLimitService.RateLimitDecision first = service.consume("bucket", bucket);
        RateLimitService.RateLimitDecision second = service.consume("bucket", bucket);

        assertThat(first.allowed()).isTrue();
        assertThat(first.remaining()).isZero();
        assertThat(second.allowed()).isFalse();
        assertThat(second.retryAfterSeconds()).isPositive();
    }

    private static final class RedisFailureRateLimitService extends RedisRateLimitService {
        private RedisFailureRateLimitService() {
            super(null);
        }

        @Override
        protected Long increment(String bucketKey) {
            throw new RedisConnectionFailureException("redis down");
        }
    }
}
