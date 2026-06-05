package com.taskmind.backend.auth.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(@NotBlank String refreshToken) {}
