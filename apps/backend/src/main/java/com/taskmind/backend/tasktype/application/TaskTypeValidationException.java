package com.taskmind.backend.tasktype.application;

import com.taskmind.backend.task.application.TaskErrorCode;
import com.taskmind.backend.task.application.TaskErrorMetadata;

public class TaskTypeValidationException extends RuntimeException {
    private final TaskErrorMetadata metadata;

    public TaskTypeValidationException(String message) {
        this(message, TaskErrorMetadata.withCode(TaskErrorCode.TASK_TYPE_VALIDATION_FAILED));
    }

    public TaskTypeValidationException(String message, TaskErrorMetadata metadata) {
        super(message);
        this.metadata = metadata == null ? TaskErrorMetadata.withCode(TaskErrorCode.TASK_TYPE_VALIDATION_FAILED) : metadata;
    }

    public TaskErrorMetadata metadata() {
        return metadata;
    }
}
