package com.taskmind.backend.task.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record SavedTaskViewRequest(
        @NotBlank @Size(max = 80) String name,
        Map<String, Object> filters) {}
