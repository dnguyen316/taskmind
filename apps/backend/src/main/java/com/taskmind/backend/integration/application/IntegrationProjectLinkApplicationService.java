package com.taskmind.backend.integration.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.integration.domain.model.*;
import com.taskmind.backend.integration.domain.repository.IntegrationProjectLinkRepository;
import com.taskmind.backend.project.application.ProjectMembershipApplicationService;
import com.taskmind.backend.project.domain.repository.ProjectRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class IntegrationProjectLinkApplicationService {
    private final IntegrationProjectLinkRepository links; private final IntegrationConnectionApplicationService connections; private final ProjectMembershipApplicationService memberships; private final ProjectRepository projects;
    public IntegrationProjectLinkApplicationService(IntegrationProjectLinkRepository links, IntegrationConnectionApplicationService connections, ProjectMembershipApplicationService memberships, ProjectRepository projects) { this.links = links; this.connections = connections; this.memberships = memberships; this.projects = projects; }
    @Transactional
    public IntegrationProjectLink link(AuthenticatedUser actor, UUID projectId, UUID connectionId, String externalProjectId, String externalProjectKey, String externalProjectName, String metadataJson) {
        if (!canAccess(actor, projectId)) throw new IllegalArgumentException("Project access denied");
        IntegrationConnection c = connections.requireOwned(actor, connectionId);
        if (externalProjectId == null || externalProjectId.isBlank()) throw new IllegalArgumentException("externalProjectId is required");
        Instant now = Instant.now();
        return links.save(new IntegrationProjectLink(UUID.randomUUID(), null, projectId, connectionId, c.provider(), externalProjectId.trim(), externalProjectKey, externalProjectName, metadataJson, actor.userId(), now, now));
    }
    public List<IntegrationProjectLink> list(AuthenticatedUser actor, UUID projectId) { if (!canAccess(actor, projectId)) throw new IllegalArgumentException("Project access denied"); return links.findByProjectId(projectId); }
    public IntegrationProjectLink requireAccessible(AuthenticatedUser actor, UUID id) { IntegrationProjectLink l = links.findById(id).orElseThrow(() -> new IllegalArgumentException("Project link not found")); if (!canAccess(actor, l.projectId())) throw new IllegalArgumentException("Project access denied"); return l; }
    private boolean canAccess(AuthenticatedUser actor, UUID projectId) { return actor.isPrivileged() || memberships.isMember(projectId, actor.userId()) || projects.findById(projectId).map(p -> p.ownerUserId().equals(actor.userId())).orElse(false); }
}
