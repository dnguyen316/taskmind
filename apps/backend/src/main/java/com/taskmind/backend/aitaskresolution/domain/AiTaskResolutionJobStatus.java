package com.taskmind.backend.aitaskresolution.domain;

public enum AiTaskResolutionJobStatus {
    QUEUED,
    RUNNING,
    WAITING_FOR_APPROVAL,
    SUCCEEDED,
    FAILED,
    CANCELED,
    PAUSED
}
