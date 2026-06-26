package com.taskmind.backend.integration.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.integration.domain.model.*;
import com.taskmind.backend.integration.domain.repository.IntegrationProjectLinkRepository;
import com.taskmind.backend.integration.infrastructure.github.GitHubClient;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GitHubIntegrationApplicationService {
    private static final List<String> DEFAULT_OPS = List.of("READ_ISSUES", "READ_CODE", "CREATE_BRANCH", "CREATE_PR", "COMMENT");
    private final GitHubClient github;
    private final IntegrationConnectionApplicationService connections;
    private final IntegrationProjectLinkApplicationService projectLinks;
    private final IntegrationProjectLinkRepository links;
    private final ObjectMapper mapper;

    public GitHubIntegrationApplicationService(GitHubClient github, IntegrationConnectionApplicationService connections, IntegrationProjectLinkApplicationService projectLinks, IntegrationProjectLinkRepository links, ObjectMapper mapper) {
        this.github = github; this.connections = connections; this.projectLinks = projectLinks; this.links = links; this.mapper = mapper;
    }

    public GitHubClient.RepositoryMetadata getRepository(AuthenticatedUser actor, UUID connectionId, String owner, String repo) {
        IntegrationConnection c = connections.requireOwned(actor, connectionId);
        if (c.provider() != IntegrationProvider.GITHUB) throw new IllegalArgumentException("Connection must be GITHUB");
        IntegrationConnectionApplicationService.ConnectionCredentials creds = connections.credentials(c);
        return github.getRepository(creds.baseUrl(), creds.accessToken(), owner, repo);
    }

    @Transactional
    public IntegrationProjectLink linkRepository(AuthenticatedUser actor, UUID projectId, UUID connectionId, String owner, String repo, List<String> allowedOperations) {
        projectLinks.list(actor, projectId);
        GitHubClient.RepositoryMetadata metadata = getRepository(actor, connectionId, owner, repo);
        IntegrationConnection c = connections.requireOwned(actor, connectionId);
        List<String> ops = allowedOperations == null || allowedOperations.isEmpty() ? DEFAULT_OPS : allowedOperations;
        Instant now = Instant.now();
        return links.save(new IntegrationProjectLink(UUID.randomUUID(), null, projectId, connectionId, c.provider(), metadata.fullName(), metadata.fullName(), metadata.name(), metadataJson(metadata), metadata.owner(), metadata.name(), metadata.defaultBranch(), metadata.installationId(), metadata.accountId(), json(ops), actor.userId(), now, now));
    }

    private String metadataJson(GitHubClient.RepositoryMetadata metadata) { return json(Map.of("owner", metadata.owner(), "repo", metadata.name(), "private", metadata.isPrivate(), "htmlUrl", metadata.htmlUrl() == null ? "" : metadata.htmlUrl())); }
    private String json(Object value) { try { return mapper.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalArgumentException("Unable to serialize GitHub metadata", e); } }
}
