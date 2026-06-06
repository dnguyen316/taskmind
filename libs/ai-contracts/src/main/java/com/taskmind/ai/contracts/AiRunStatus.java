package com.taskmind.ai.contracts;

/** Lifecycle status for a Nova AI run. */
public enum AiRunStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
