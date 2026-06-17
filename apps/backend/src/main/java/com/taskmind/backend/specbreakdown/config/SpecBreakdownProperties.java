package com.taskmind.backend.specbreakdown.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="taskmind.spec-breakdown") public record SpecBreakdownProperties(int queueLimit,int concurrency,int timeoutSeconds,int maxRetries){ public SpecBreakdownProperties(){this(100,2,900,2);} }
