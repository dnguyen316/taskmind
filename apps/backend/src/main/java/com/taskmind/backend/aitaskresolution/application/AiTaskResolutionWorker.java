package com.taskmind.backend.aitaskresolution.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AiTaskResolutionWorker {
    private final AiTaskResolutionApplicationService service;
    public AiTaskResolutionWorker(AiTaskResolutionApplicationService service) { this.service = service; }
    @Scheduled(fixedDelayString = "${taskmind.ai-task-resolution.worker-delay-ms:1000}")
    public void processQueuedJobs() { while (service.processOneQueuedJob()) {} }
}
