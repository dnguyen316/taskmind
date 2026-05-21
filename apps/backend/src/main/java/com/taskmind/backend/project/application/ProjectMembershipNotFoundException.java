package com.taskmind.backend.project.application;

public class ProjectMembershipNotFoundException extends RuntimeException {

    public ProjectMembershipNotFoundException(String message) {
        super(message);
    }
}
