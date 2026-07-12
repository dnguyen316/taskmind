package com.taskmind.backend.task.application;

public class TaskAccessDeniedException extends RuntimeException {
    public TaskAccessDeniedException(String message) {
        super(message);
    }
}
