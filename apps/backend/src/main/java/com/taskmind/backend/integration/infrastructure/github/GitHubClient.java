package com.taskmind.backend.integration.infrastructure.github;

import static com.taskmind.backend.integration.infrastructure.ProviderHttpSupport.map;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GitHubClient {
    private final RestClient restClient;

    public GitHubClient(RestClient.Builder builder) { this.restClient = builder.build(); }

    public RepositoryMetadata getRepository(String baseUrl, String accessToken, String owner, String repo) {
        JsonNode node = get(apiBaseUrl(baseUrl), accessToken, "/repos/{owner}/{repo}", owner, repo);
        return new RepositoryMetadata(node.path("node_id").asText(node.path("id").asText()), node.path("owner").path("login").asText(owner), node.path("name").asText(repo), node.path("full_name").asText(owner + "/" + repo), node.path("default_branch").asText(null), node.path("private").asBoolean(false), node.path("html_url").asText(null), node.path("installation").path("id").asText(null), node.path("owner").path("id").asText(null));
    }

    public Issue getIssue(String baseUrl, String accessToken, String owner, String repo, int issueNumber) {
        JsonNode node = get(apiBaseUrl(baseUrl), accessToken, "/repos/{owner}/{repo}/issues/{issueNumber}", owner, repo, issueNumber);
        return issue(node);
    }

    public List<Comment> listIssueComments(String baseUrl, String accessToken, String owner, String repo, int issueNumber) {
        JsonNode response = get(apiBaseUrl(baseUrl), accessToken, "/repos/{owner}/{repo}/issues/{issueNumber}/comments", owner, repo, issueNumber);
        if (response == null || !response.isArray()) return List.of();
        return java.util.stream.StreamSupport.stream(response.spliterator(), false).map(this::comment).toList();
    }

    public Comment createIssueComment(String baseUrl, String accessToken, String owner, String repo, int issueNumber, String body) {
        try {
            JsonNode node = restClient.post().uri(apiBaseUrl(baseUrl), uri -> uri.path("/repos/{owner}/{repo}/issues/{issueNumber}/comments").build(owner, repo, issueNumber)).headers(headers -> headers.setBearerAuth(accessToken)).body(new CommentRequest(body)).retrieve().body(JsonNode.class);
            return comment(node);
        } catch (RestClientException exception) { throw map("GitHub", exception); }
    }

    public Ref createBranchRef(String baseUrl, String accessToken, String owner, String repo, String branchName, String fromSha) {
        try {
            JsonNode node = restClient.post().uri(apiBaseUrl(baseUrl), uri -> uri.path("/repos/{owner}/{repo}/git/refs").build(owner, repo)).headers(headers -> headers.setBearerAuth(accessToken)).body(new RefRequest("refs/heads/" + branchName, fromSha)).retrieve().body(JsonNode.class);
            return new Ref(node.path("ref").asText(), node.path("object").path("sha").asText());
        } catch (RestClientException exception) { throw map("GitHub", exception); }
    }

    public PullRequest createPullRequest(String baseUrl, String accessToken, String owner, String repo, String title, String head, String base, String body) {
        try {
            return pullRequest(restClient.post().uri(apiBaseUrl(baseUrl), uri -> uri.path("/repos/{owner}/{repo}/pulls").build(owner, repo)).headers(headers -> headers.setBearerAuth(accessToken)).body(new PullRequestRequest(title, head, base, body)).retrieve().body(JsonNode.class));
        } catch (RestClientException exception) { throw map("GitHub", exception); }
    }

    public PullRequestStatus getPullRequestStatus(String baseUrl, String accessToken, String owner, String repo, int pullNumber) {
        JsonNode pr = get(apiBaseUrl(baseUrl), accessToken, "/repos/{owner}/{repo}/pulls/{pullNumber}", owner, repo, pullNumber);
        return new PullRequestStatus(pr.path("node_id").asText(pr.path("id").asText()), pr.path("number").asInt(pullNumber), pr.path("state").asText(), pr.path("mergeable").isMissingNode() ? null : pr.path("mergeable").asBoolean(), pr.path("merged").asBoolean(false), pr.path("head").path("sha").asText(null));
    }

    public List<ExternalIssue> importIssues(String baseUrl, String accessToken, String repository, int limit) {
        try {
            String[] repoParts = repository.split("/", 2);
            if (repoParts.length != 2 || repoParts[0].isBlank() || repoParts[1].isBlank()) throw new IllegalArgumentException("GitHub repository must be in owner/name form");
            JsonNode response = restClient.get().uri(apiBaseUrl(baseUrl), uri -> uri.path("/repos/{owner}/{repo}/issues").queryParam("state", "open").queryParam("per_page", Math.max(limit, 0)).build(repoParts[0], repoParts[1])).headers(headers -> headers.setBearerAuth(accessToken)).retrieve().body(JsonNode.class);
            if (response == null || !response.isArray()) return List.of();
            return java.util.stream.StreamSupport.stream(response.spliterator(), false).filter(issue -> !issue.has("pull_request")).map(issue -> new ExternalIssue(issue.path("node_id").asText(issue.path("id").asText()), "#" + issue.path("number").asText(), issue.path("title").asText(), issue.path("body").asText(null))).toList();
        } catch (RestClientException exception) { throw map("GitHub", exception); }
    }

    private JsonNode get(String apiBaseUrl, String accessToken, String path, Object... args) { try { return restClient.get().uri(apiBaseUrl, uri -> uri.path(path).build(args)).headers(headers -> headers.setBearerAuth(accessToken)).retrieve().body(JsonNode.class); } catch (RestClientException exception) { throw map("GitHub", exception); } }
    private String apiBaseUrl(String baseUrl) { return baseUrl == null || baseUrl.isBlank() ? "https://api.github.com" : baseUrl; }
    private Issue issue(JsonNode n) { return new Issue(n.path("node_id").asText(n.path("id").asText()), n.path("number").asInt(), n.path("title").asText(), n.path("body").asText(null), n.path("state").asText(), n.path("html_url").asText(null)); }
    private Comment comment(JsonNode n) { return new Comment(n.path("node_id").asText(n.path("id").asText()), n.path("body").asText(), n.path("user").path("login").asText(null), n.path("html_url").asText(null)); }
    private PullRequest pullRequest(JsonNode n) { return new PullRequest(n.path("node_id").asText(n.path("id").asText()), n.path("number").asInt(), n.path("title").asText(), n.path("state").asText(), n.path("html_url").asText(null), n.path("head").path("sha").asText(null)); }

    public record RepositoryMetadata(String id, String owner, String name, String fullName, String defaultBranch, boolean isPrivate, String htmlUrl, String installationId, String accountId) {}
    public record Issue(String id, int number, String title, String body, String state, String htmlUrl) {}
    public record Comment(String id, String body, String authorLogin, String htmlUrl) {}
    public record Ref(String ref, String sha) {}
    public record PullRequest(String id, int number, String title, String state, String htmlUrl, String headSha) {}
    public record PullRequestStatus(String id, int number, String state, Boolean mergeable, boolean merged, String headSha) {}
    public record ExternalIssue(String id, String key, String title, String description) {}
    private record CommentRequest(String body) {}
    private record RefRequest(String ref, String sha) {}
    private record PullRequestRequest(String title, String head, String base, String body) {}
}
