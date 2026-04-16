package com.taskmind.backend.project.application;

public record UpdateProjectCommand(
    String name,
    String key,
    String description
) {
}
