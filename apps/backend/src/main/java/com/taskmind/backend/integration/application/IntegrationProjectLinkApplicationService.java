package com.taskmind.backend.integration.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.integration.domain.model.IntegrationConnection;
import com.taskmind.backend.integration.domain.model.IntegrationProjectLink;
import com.taskmind.backend.integration.domain.repository.IntegrationProjectLinkRepository;
import com.taskmind.backend.project.application.ProjectMembershipApplicationService;
import com.taskmind.backend.project.domain.repository.ProjectRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IntegrationProjectLinkApplicationService {
    private final IntegrationProjectLinkRepository links;
    private final IntegrationConnectionApplicationService connections;
    private final ProjectMembershipApplicationService memberships;
    private final ProjectRepository projects;

    public IntegrationProjectLinkApplicationService(
            IntegrationProjectLinkRepository links,
            IntegrationConnectionApplicationService connections,
            ProjectMembershipApplicationService memberships,
            ProjectRepository projects) {
        this.links = links;
        this.connections = connections;
        this.memberships = memberships;
        this.projects = projects;
    }

    @Transactional
    public IntegrationProjectLink link(
            AuthenticatedUser actor,
            UUID projectId,
            UUID connectionId,
            String externalProjectId,
            String externalProjectKey,
            String externalProjectName,
            String metadataJson) {
        if (!canAccess(actor, projectId)) {
            throw new IllegalArgumentException("Project access denied");
        }
        IntegrationConnection connection = connections.requireOwned(actor, connectionId);
        if (externalProjectId == null || externalProjectId.isBlank()) {
            throw new IllegalArgumentException("externalProjectId is required");
        }
        Instant now = Instant.now();
        return links.save(new IntegrationProjectLink(
                UUID.randomUUID(),
                null,
                projectId,
                connectionId,
                connection.provider(),
                externalProjectId.trim(),
                externalProjectKey,
                externalProjectName,
                metadataJson,
                null,
                null,
                null,
                null,
                null,
                null,
                actor.userId(),
                now,
                now));
    }

    public List<IntegrationProjectLink> list(AuthenticatedUser actor, UUID projectId) {
        if (!canAccess(actor, projectId)) {
            throw new IllegalArgumentException("Project access denied");
        }
        return links.findByProjectId(projectId);
    }

    public IntegrationProjectLink requireAccessible(AuthenticatedUser actor, UUID projectLinkId) {
        IntegrationProjectLink link = links.findById(projectLinkId)
                .orElseThrow(() -> new IllegalArgumentException("Project link not found"));
        if (!canAccess(actor, link.projectId())) {
            throw new IllegalArgumentException("Project access denied");
        }
        return link;
    }

    private boolean canAccess(AuthenticatedUser actor, UUID projectId) {
        return actor.isPrivileged()
                || memberships.isMember(projectId, actor.userId())
                || projects.findById(projectId).map(project -> project.ownerUserId().equals(actor.userId())).orElse(false);
    }
}
