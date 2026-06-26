package com.taskmind.backend.integration.interfaces.rest;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.integration.application.*;
import com.taskmind.backend.integration.domain.model.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v1/integrations")
@Validated
public class IntegrationController {
    private final IntegrationConnectionApplicationService connections; private final IntegrationProjectLinkApplicationService links; private final IntegrationImportApplicationService imports; private final GitHubIntegrationApplicationService github;
    public IntegrationController(IntegrationConnectionApplicationService connections, IntegrationProjectLinkApplicationService links, IntegrationImportApplicationService imports, GitHubIntegrationApplicationService github) { this.connections = connections; this.links = links; this.imports = imports; this.github = github; }

    @PostMapping("/{provider}/connections")
    public ResponseEntity<ConnectionResponse> connect(@PathVariable IntegrationProvider provider, AuthenticatedUser actor, @Valid @RequestBody ConnectRequest request) {
        try { return ResponseEntity.status(HttpStatus.CREATED).body(ConnectionResponse.from(connections.connect(actor, provider, request.accountName(), request.baseUrl(), request.accountExternalId(), request.accessToken(), request.refreshToken(), request.scopes()))); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e); }
    }
    @GetMapping("/connections") public List<ConnectionResponse> listConnections(AuthenticatedUser actor) { return connections.list(actor).stream().map(ConnectionResponse::from).toList(); }
    @PostMapping("/projects/{projectId}/links") public ResponseEntity<ProjectLinkResponse> linkProject(@PathVariable UUID projectId, AuthenticatedUser actor, @Valid @RequestBody ProjectLinkRequest request) { try { return ResponseEntity.status(HttpStatus.CREATED).body(ProjectLinkResponse.from(links.link(actor, projectId, request.connectionId(), request.externalProjectId(), request.externalProjectKey(), request.externalProjectName(), request.metadataJson()))); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e); } }
    @GetMapping("/projects/{projectId}/links") public List<ProjectLinkResponse> listProjectLinks(@PathVariable UUID projectId, AuthenticatedUser actor) { try { return links.list(actor, projectId).stream().map(ProjectLinkResponse::from).toList(); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e); } }
    @GetMapping("/github/repositories/{owner}/{repo}") public GitHubRepositoryResponse getGitHubRepository(@RequestParam UUID connectionId, @PathVariable String owner, @PathVariable String repo, AuthenticatedUser actor) { try { return GitHubRepositoryResponse.from(github.getRepository(actor, connectionId, owner, repo)); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e); } }
    @PostMapping("/github/projects/{projectId}/repositories") public ResponseEntity<ProjectLinkResponse> linkGitHubRepository(@PathVariable UUID projectId, AuthenticatedUser actor, @Valid @RequestBody GitHubRepositoryLinkRequest request) { try { return ResponseEntity.status(HttpStatus.CREATED).body(ProjectLinkResponse.from(github.linkRepository(actor, projectId, request.connectionId(), request.owner(), request.repo(), request.allowedOperations()))); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage(), e); } }

    @PostMapping("/project-links/{projectLinkId}/imports") public ImportRunResponse importIssues(@PathVariable UUID projectLinkId, AuthenticatedUser actor, @Valid @RequestBody ImportRequest request) { try { return ImportRunResponse.from(imports.importIssues(actor, projectLinkId, request.limit() == null ? 0 : request.limit())); } catch (IllegalArgumentException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e); } }

    public record ConnectRequest(@NotBlank @Size(max=255) String accountName, @Size(max=500) String baseUrl, @Size(max=255) String accountExternalId, @NotBlank String accessToken, String refreshToken, String scopes) {}
    public record ProjectLinkRequest(@NotNull UUID connectionId, @NotBlank String externalProjectId, String externalProjectKey, String externalProjectName, String metadataJson) {}
    public record ImportRequest(@Min(1) @Max(500) Integer limit) {}
    public record GitHubRepositoryLinkRequest(@NotNull UUID connectionId, @NotBlank String owner, @NotBlank String repo, List<String> allowedOperations) {}
    public record ConnectionResponse(UUID id, IntegrationProvider provider, String accountName, String baseUrl, String accountExternalId, UUID ownerUserId, String scopes, String status) { static ConnectionResponse from(IntegrationConnection c) { return new ConnectionResponse(c.id(), c.provider(), c.accountName(), c.baseUrl(), c.accountExternalId(), c.ownerUserId(), c.scopes(), c.status()); } }
    public record ProjectLinkResponse(UUID id, UUID projectId, UUID connectionId, IntegrationProvider provider, String externalProjectId, String externalProjectKey, String externalProjectName, String metadataJson, String repositoryOwner, String repositoryName, String defaultBranch, String installationId, String accountId, String allowedOperationsJson) { static ProjectLinkResponse from(IntegrationProjectLink l) { return new ProjectLinkResponse(l.id(), l.projectId(), l.connectionId(), l.provider(), l.externalProjectId(), l.externalProjectKey(), l.externalProjectName(), l.metadataJson(), l.repositoryOwner(), l.repositoryName(), l.defaultBranch(), l.installationId(), l.accountId(), l.allowedOperationsJson()); } }
    public record GitHubRepositoryResponse(String id, String owner, String name, String fullName, String defaultBranch, boolean isPrivate, String htmlUrl, String installationId, String accountId) { static GitHubRepositoryResponse from(com.taskmind.backend.integration.infrastructure.github.GitHubClient.RepositoryMetadata r) { return new GitHubRepositoryResponse(r.id(), r.owner(), r.name(), r.fullName(), r.defaultBranch(), r.isPrivate(), r.htmlUrl(), r.installationId(), r.accountId()); } }
    public record ImportRunResponse(UUID id, UUID projectId, UUID projectLinkId, IntegrationProvider provider, String status, int importedCount, int skippedCount) { static ImportRunResponse from(IntegrationImportRun r) { return new ImportRunResponse(r.id(), r.projectId(), r.projectLinkId(), r.provider(), r.status(), r.importedCount(), r.skippedCount()); } }
}
