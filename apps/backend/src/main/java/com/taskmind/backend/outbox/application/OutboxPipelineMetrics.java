package com.taskmind.backend.outbox.application;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class OutboxPipelineMetrics {
    private final AtomicLong published = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    public void recordPublished() {
        published.incrementAndGet();
    }

    public void recordFailed() {
        failed.incrementAndGet();
    }

    public long publishedCount() {
        return published.get();
    }

    public long failedCount() {
        return failed.get();
    }
}
