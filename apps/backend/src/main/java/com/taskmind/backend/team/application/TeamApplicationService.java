package com.taskmind.backend.team.application;

import com.taskmind.backend.analytics.application.AnalyticsRollupRepository;
import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserJpaRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TeamApplicationService {
    private final UserJpaRepository users;
    private final AnalyticsRollupRepository analytics;

    public TeamApplicationService(UserJpaRepository users, AnalyticsRollupRepository analytics) {
        this.users = users;
        this.analytics = analytics;
    }

    public TeamDirectoryResponse directory(AuthenticatedUser requester) {
        if (!requester.isPrivileged())
            throw new SecurityException("Team directory requires ADMIN or MANAGER role");
        Map<java.util.UUID, Integer> workload =
                analytics.assigneeWorkload().stream()
                        .collect(Collectors.toMap(w -> w.userId(), w -> w.openTasks()));
        List<TeamMemberResponse> members =
                users.findAll().stream()
                        .map(
                                u ->
                                        new TeamMemberResponse(
                                                u.getId(),
                                                u.getDisplayName(),
                                                u.getPrimaryEmail(),
                                                workload.getOrDefault(u.getId(), 0)))
                        .toList();
        return new TeamDirectoryResponse(
                members,
                members.size(),
                members.stream().mapToInt(TeamMemberResponse::openTasks).sum());
    }
}
