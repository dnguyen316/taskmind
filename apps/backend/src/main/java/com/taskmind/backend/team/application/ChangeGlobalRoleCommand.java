package com.taskmind.backend.team.application;

import java.util.UUID;

public record ChangeGlobalRoleCommand(UUID userId, String role) {}
