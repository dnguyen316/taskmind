package com.taskmind.backend.tasktype.application;

public class TaskTypeForbiddenException extends RuntimeException {
    public TaskTypeForbiddenException(String message) {
        super(message);
    }
}
