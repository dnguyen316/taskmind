package com.taskmind.backend.team.application;

import java.util.UUID;

public record TeamMemberResponse(UUID userId, String displayName, String email, int openTasks) {}
