package com.taskmind.backend.team.application;

import java.util.UUID;

public record RemoveProjectMemberCommand(UUID userId, UUID projectId) {}
