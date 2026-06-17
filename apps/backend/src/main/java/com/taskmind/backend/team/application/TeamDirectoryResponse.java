package com.taskmind.backend.team.application;

import java.util.List;

public record TeamDirectoryResponse(
        List<TeamMemberResponse> members, int totalMembers, int totalOpenTasks) {}
