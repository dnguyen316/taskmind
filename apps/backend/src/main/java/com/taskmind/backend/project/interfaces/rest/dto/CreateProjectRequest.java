package com.taskmind.backend.project.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateProjectRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 20) String key,
    String description,
    @NotNull UUID ownerUserId
) {
}
