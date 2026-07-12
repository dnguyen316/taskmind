package com.taskmind.backend.tasktype.application;

public class TaskTypeValidationException extends RuntimeException {
    public TaskTypeValidationException(String message) {
        super(message);
    }
}
