package com.taskmind.backend.specbreakdown.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SpecBreakdownWorker {
    private final SpecBreakdownApplicationService service;

    public SpecBreakdownWorker(SpecBreakdownApplicationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${taskmind.spec-breakdown.worker-delay-ms:1000}")
    public void processQueuedJobs() {
        while (service.processOneQueuedJob()) {
            // Drain currently queued work without waiting for the next scheduler tick.
        }
    }
}
