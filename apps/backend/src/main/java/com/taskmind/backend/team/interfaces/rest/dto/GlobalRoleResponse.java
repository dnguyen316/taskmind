package com.taskmind.backend.team.interfaces.rest.dto;

import java.util.UUID;

public record GlobalRoleResponse(UUID userId, String role) {}
