package com.taskmind.backend.integration.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.integration.domain.model.IntegrationConnection;
import com.taskmind.backend.integration.domain.model.IntegrationProjectLink;
import com.taskmind.backend.integration.domain.model.IntegrationProvider;
import com.taskmind.backend.integration.domain.repository.IntegrationProjectLinkRepository;
import com.taskmind.backend.integration.infrastructure.github.GitHubClient;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GitHubToolApplicationService {
    private final IntegrationProjectLinkRepository links;
    private final IntegrationConnectionApplicationService connections;
    private final GitHubClient github;
    private final ObjectMapper mapper;
    private final Map<String, Object> idempotencyResults = new ConcurrentHashMap<>();

    public GitHubToolApplicationService(IntegrationProjectLinkRepository links, IntegrationConnectionApplicationService connections, GitHubClient github, ObjectMapper mapper) {
        this.links = links;
        this.connections = connections;
        this.github = github;
        this.mapper = mapper;
    }

    public GitHubClient.Issue getIssue(UUID linkId, UUID userId, UUID projectId, int issueNumber) {
        Context context = context(linkId, userId, projectId, "READ_ISSUES");
        return github.getIssue(context.credentials().baseUrl(), context.credentials().accessToken(), context.link().repositoryOwner(), context.link().repositoryName(), issueNumber);
    }

    public GitHubClient.PullRequestStatus getPullRequest(UUID linkId, UUID userId, UUID projectId, int pullNumber) {
        Context context = context(linkId, userId, projectId, "READ_CODE");
        return github.getPullRequestStatus(context.credentials().baseUrl(), context.credentials().accessToken(), context.link().repositoryOwner(), context.link().repositoryName(), pullNumber);
    }

    public GitHubClient.Comment createComment(UUID linkId, UUID userId, UUID projectId, String idempotencyKey, int issueNumber, String body) {
        requireBody(body, "body");
        return idempotent(linkId, idempotencyKey, "comments", () -> {
            Context context = context(linkId, userId, projectId, "COMMENT");
            return github.createIssueComment(context.credentials().baseUrl(), context.credentials().accessToken(), context.link().repositoryOwner(), context.link().repositoryName(), issueNumber, body);
        }, GitHubClient.Comment.class);
    }

    public GitHubClient.Ref createBranch(UUID linkId, UUID userId, UUID projectId, String idempotencyKey, String branchName, String fromSha) {
        requireBody(branchName, "branchName");
        requireBody(fromSha, "fromSha");
        return idempotent(linkId, idempotencyKey, "branches", () -> {
            Context context = context(linkId, userId, projectId, "CREATE_BRANCH");
            return github.createBranchRef(context.credentials().baseUrl(), context.credentials().accessToken(), context.link().repositoryOwner(), context.link().repositoryName(), branchName, fromSha);
        }, GitHubClient.Ref.class);
    }

    public GitHubClient.PullRequest createPullRequest(UUID linkId, UUID userId, UUID projectId, String idempotencyKey, String title, String head, String base, String body) {
        requireBody(title, "title");
        requireBody(head, "head");
        requireBody(base, "base");
        return idempotent(linkId, idempotencyKey, "pull-requests", () -> {
            Context context = context(linkId, userId, projectId, "CREATE_PR");
            return github.createPullRequest(context.credentials().baseUrl(), context.credentials().accessToken(), context.link().repositoryOwner(), context.link().repositoryName(), title, head, base, body);
        }, GitHubClient.PullRequest.class);
    }

    private Context context(UUID linkId, UUID userId, UUID projectId, String operation) {
        IntegrationProjectLink link = links.findById(linkId).orElseThrow(() -> new GitHubToolException.NotFound("GitHub repository link not found"));
        if (link.provider() != IntegrationProvider.GITHUB) throw new GitHubToolException.NotFound("GitHub repository link not found");
        if (!link.projectId().equals(projectId) || !link.createdBy().equals(userId)) throw new GitHubToolException.Forbidden("GitHub repository link is outside the supplied job scope");
        if (link.repositoryOwner() == null || link.repositoryName() == null) throw new GitHubToolException.NotFound("GitHub repository metadata is incomplete");
        if (!allowedOperations(link).contains(operation)) throw new GitHubToolException.Forbidden("GitHub repository link does not allow " + operation);
        IntegrationConnection connection =
                connections.requireOwned(
                        new com.taskmind.backend.auth.AuthenticatedUser(userId, java.util.Set.of()), link.connectionId());
        return new Context(link, connections.credentials(connection));
    }

    private List<String> allowedOperations(IntegrationProjectLink link) {
        if (link.allowedOperationsJson() == null || link.allowedOperationsJson().isBlank()) return List.of();
        try {
            return mapper.readValue(link.allowedOperationsJson(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid GitHub operation flags");
        }
    }

    private <T> T idempotent(UUID linkId, String key, String operation, Supplier<T> supplier, Class<T> type) {
        requireBody(key, "Idempotency-Key");
        Object result = idempotencyResults.computeIfAbsent(linkId + ":" + operation + ":" + key.trim(), ignored -> supplier.get());
        return type.cast(result);
    }

    private void requireBody(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    }

    private record Context(IntegrationProjectLink link, IntegrationConnectionApplicationService.ConnectionCredentials credentials) {}

    public static class GitHubToolException extends RuntimeException {
        GitHubToolException(String message) { super(message); }
        public static final class Forbidden extends GitHubToolException { public Forbidden(String message) { super(message); } }
        public static final class NotFound extends GitHubToolException { public NotFound(String message) { super(message); } }
    }
}
