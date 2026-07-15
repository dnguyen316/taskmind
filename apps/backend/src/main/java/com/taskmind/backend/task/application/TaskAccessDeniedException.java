package com.taskmind.backend.task.application;

public class TaskAccessDeniedException extends RuntimeException {
    private final TaskErrorMetadata metadata;

    public TaskAccessDeniedException(String message) {
        this(message, TaskErrorMetadata.withCode(TaskErrorCode.TASK_ACCESS_DENIED));
    }

    public TaskAccessDeniedException(String message, TaskErrorMetadata metadata) {
        super(message);
        this.metadata = metadata == null ? TaskErrorMetadata.withCode(TaskErrorCode.TASK_ACCESS_DENIED) : metadata;
    }

    public TaskErrorMetadata metadata() {
        return metadata;
    }
}
