package com.taskmind.backend.project.application;

public class ProjectMembershipForbiddenException extends RuntimeException {

    public ProjectMembershipForbiddenException(String message) {
        super(message);
    }
}
