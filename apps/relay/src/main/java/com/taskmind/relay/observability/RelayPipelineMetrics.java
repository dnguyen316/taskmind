package com.taskmind.relay.observability;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class RelayPipelineMetrics {
    private final AtomicLong ingested = new AtomicLong();
    private final AtomicLong duplicates = new AtomicLong();
    private final AtomicLong deadLetters = new AtomicLong();

    public void recordIngested() { ingested.incrementAndGet(); }

    public void recordDuplicate() { duplicates.incrementAndGet(); }

    public void recordDeadLetter() { deadLetters.incrementAndGet(); }

    public long ingestedCount() { return ingested.get(); }

    public long duplicateCount() { return duplicates.get(); }

    public long deadLetterCount() { return deadLetters.get(); }
}
