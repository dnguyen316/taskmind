package com.taskmind.backend.tasktype.application;

import com.taskmind.backend.task.application.TaskErrorCode;
import com.taskmind.backend.task.application.TaskErrorMetadata;
import java.util.UUID;

public class TaskTypeValidationException extends RuntimeException {
    private final TaskErrorMetadata metadata;
    private final String field;
    private final String reason;
    private final UUID projectId;

    public TaskTypeValidationException(String message) {
        this(message, TaskErrorMetadata.withCode(TaskErrorCode.TASK_TYPE_VALIDATION_FAILED));
    }

    public TaskTypeValidationException(String message, String field, String reason) {
        this(message, field, reason, null);
    }

    public TaskTypeValidationException(String message, String field, String reason, UUID projectId) {
        this(
                message,
                new TaskErrorMetadata(
                        TaskErrorCode.TASK_TYPE_VALIDATION_FAILED, null, null, null, field, reason),
                field,
                reason,
                projectId);
    }

    public TaskTypeValidationException(String message, TaskErrorMetadata metadata) {
        this(message, metadata, metadata == null ? null : metadata.field(), metadata == null ? null : metadata.reason(), null);
    }

    private TaskTypeValidationException(
            String message, TaskErrorMetadata metadata, String field, String reason, UUID projectId) {
        super(message);
        this.metadata = metadata == null ? TaskErrorMetadata.withCode(TaskErrorCode.TASK_TYPE_VALIDATION_FAILED) : metadata;
        this.field = blankToNull(field);
        this.reason = blankToNull(reason);
        this.projectId = projectId;
    }

    public TaskErrorMetadata metadata() {
        return metadata;
    }

    public String field() {
        return field;
    }

    public String reason() {
        return reason;
    }

    public UUID projectId() {
        return projectId;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
