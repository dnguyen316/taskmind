package com.taskmind.backend.task.application;

public class TaskValidationException extends RuntimeException {
    private final TaskErrorMetadata metadata;

    public TaskValidationException(String message) {
        this(message, TaskErrorMetadata.withCode(TaskErrorCode.TASK_VALIDATION_FAILED));
    }

    public TaskValidationException(String message, TaskErrorMetadata metadata) {
        super(message);
        this.metadata = metadata == null ? TaskErrorMetadata.withCode(TaskErrorCode.TASK_VALIDATION_FAILED) : metadata;
    }

    public TaskValidationException(String message, Throwable cause) {
        this(message, cause, TaskErrorMetadata.withCode(TaskErrorCode.TASK_VALIDATION_FAILED));
    }

    public TaskValidationException(String message, Throwable cause, TaskErrorMetadata metadata) {
        super(message, cause);
        this.metadata = metadata == null ? TaskErrorMetadata.withCode(TaskErrorCode.TASK_VALIDATION_FAILED) : metadata;
    }

    public String reason() {
        return metadata.reason();
    }

    public TaskErrorMetadata metadata() {
        return metadata;
    }
}
