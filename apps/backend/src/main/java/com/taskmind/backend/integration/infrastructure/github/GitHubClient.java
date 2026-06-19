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

    public GitHubClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public List<ExternalIssue> importIssues(String baseUrl, String accessToken, String repository, int limit) {
        try {
            String apiBaseUrl = baseUrl == null || baseUrl.isBlank() ? "https://api.github.com" : baseUrl;
            String[] repoParts = repository.split("/", 2);
            if (repoParts.length != 2 || repoParts[0].isBlank() || repoParts[1].isBlank()) {
                throw new IllegalArgumentException("GitHub repository must be in owner/name form");
            }
            JsonNode response = restClient.get()
                    .uri(apiBaseUrl, uri -> uri.path("/repos/{owner}/{repo}/issues")
                            .queryParam("state", "open")
                            .queryParam("per_page", Math.max(limit, 0))
                            .build(repoParts[0], repoParts[1]))
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.isArray()) return List.of();
            return java.util.stream.StreamSupport.stream(response.spliterator(), false)
                    .filter(issue -> !issue.has("pull_request"))
                    .map(issue -> new ExternalIssue(
                            issue.path("node_id").asText(issue.path("id").asText()),
                            "#" + issue.path("number").asText(),
                            issue.path("title").asText(),
                            issue.path("body").asText(null)))
                    .toList();
        } catch (RestClientException exception) {
            throw map("GitHub", exception);
        }
    }

    public record ExternalIssue(String id, String key, String title, String description) {}
}
