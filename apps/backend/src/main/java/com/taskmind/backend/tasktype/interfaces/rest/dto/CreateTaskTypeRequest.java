package com.taskmind.backend.tasktype.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateTaskTypeRequest(UUID projectId, @NotBlank String key, @NotBlank String name, String color, String icon, Integer sortOrder) {}
