package com.taskmind.backend.project.interfaces.rest.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record UpdateProjectRequest(
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    @Size(max = 200) String name,
    @Pattern(regexp = ".*\\S.*", message = "must not be blank")
    @Size(max = 20) String key,
    String description
) {
}
