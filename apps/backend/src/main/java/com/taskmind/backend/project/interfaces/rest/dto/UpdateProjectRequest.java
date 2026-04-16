package com.taskmind.backend.project.interfaces.rest.dto;

import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
    @Size(max = 200) String name,
    @Size(max = 20) String key,
    String description
) {
}
