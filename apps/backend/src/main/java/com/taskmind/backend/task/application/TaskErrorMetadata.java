package com.taskmind.backend.task.application;

public record TaskErrorMetadata(
        TaskErrorCode code,
        String resource,
        String resourceId,
        String operation,
        String field,
        String reason) {
    public TaskErrorMetadata {
        code = code == null ? TaskErrorCode.TASK_VALIDATION_FAILED : code;
        resource = blankToNull(resource);
        resourceId = blankToNull(resourceId);
        operation = blankToNull(operation);
        field = blankToNull(field);
        reason = blankToNull(reason);
    }

    public static TaskErrorMetadata withCode(TaskErrorCode code) {
        return new TaskErrorMetadata(code, null, null, null, null, null);
    }

    public TaskErrorMetadata sanitizedForAccessDenied() {
        return new TaskErrorMetadata(code, resource, null, operation, field, reason);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
