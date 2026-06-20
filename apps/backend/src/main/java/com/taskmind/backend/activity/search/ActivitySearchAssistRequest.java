package com.taskmind.backend.activity.search;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivitySearchAssistRequest(
        @NotBlank @Size(max = 500) String prompt, @Size(max = 300) String currentQuery) {}
