package com.taskmind.backend.ratelimit;

public interface RateLimitService {
    RateLimitDecision consume(String bucketKey, RateLimitProperties.Bucket bucket);

    record RateLimitDecision(boolean allowed, long limit, long remaining, long retryAfterSeconds) {
        public static RateLimitDecision allowed(long limit, long remaining) {
            return new RateLimitDecision(true, limit, remaining, 0);
        }

        public static RateLimitDecision rejected(long limit, long retryAfterSeconds) {
            return new RateLimitDecision(false, limit, 0, retryAfterSeconds);
        }
    }
}
