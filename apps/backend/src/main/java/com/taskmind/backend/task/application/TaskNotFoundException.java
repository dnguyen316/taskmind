package com.taskmind.backend.task.application;

public class TaskNotFoundException extends RuntimeException {
    private final TaskErrorMetadata metadata;

    public TaskNotFoundException(String message) {
        this(message, TaskErrorMetadata.withCode(TaskErrorCode.TASK_NOT_FOUND));
    }

    public TaskNotFoundException(String message, TaskErrorMetadata metadata) {
        super(message);
        this.metadata = metadata == null ? TaskErrorMetadata.withCode(TaskErrorCode.TASK_NOT_FOUND) : metadata;
    }

    public TaskErrorMetadata metadata() {
        return metadata;
    }
}
