package com.taskmind.backend.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    @Bean
    RateLimitService rateLimitService(StringRedisTemplate redisTemplate) {
        return new RedisRateLimitService(redisTemplate);
    }

    @Bean
    ClientIpResolver clientIpResolver(RateLimitProperties properties) {
        return new ClientIpResolver(properties);
    }

    @Bean
    RateLimitFilter rateLimitFilter(
            RateLimitProperties properties,
            RateLimitService rateLimitService,
            ObjectMapper objectMapper,
            ClientIpResolver clientIpResolver) {
        return new RateLimitFilter(properties, rateLimitService, objectMapper, clientIpResolver);
    }
}
