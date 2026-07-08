package com.taskmind.backend.team.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeGlobalRoleRequest(@NotBlank String role) {}
