package com.taskmind.backend.integration.infrastructure.jira;

import static com.taskmind.backend.integration.infrastructure.ProviderHttpSupport.map;
import static com.taskmind.backend.integration.infrastructure.ProviderHttpSupport.text;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class JiraCloudClient {
    private final RestClient restClient;

    public JiraCloudClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public List<ExternalIssue> importIssues(String baseUrl, String accessToken, String externalProjectKey, int limit) {
        try {
            JsonNode response = restClient.get()
                    .uri(baseUrl, uri -> uri.path("/rest/api/3/search")
                            .queryParam("jql", "project = " + externalProjectKey + " ORDER BY updated DESC")
                            .queryParam("maxResults", Math.max(limit, 0))
                            .queryParam("fields", "summary,description")
                            .build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(JsonNode.class);
            return issues(response);
        } catch (RestClientException exception) {
            throw map("Jira", exception);
        }
    }

    private List<ExternalIssue> issues(JsonNode response) {
        if (response == null || !response.path("issues").isArray()) return List.of();
        return java.util.stream.StreamSupport.stream(response.path("issues").spliterator(), false)
                .map(issue -> new ExternalIssue(
                        issue.path("id").asText(),
                        issue.path("key").asText(),
                        issue.path("fields").path("summary").asText(),
                        text(issue.path("fields").path("description"))))
                .toList();
    }

    public PublishedIssue publish(String baseUrl, String accessToken, String projectKey, String title, String type) {
        try {
            JsonNode response = restClient.post()
                    .uri(baseUrl, uri -> uri.path("/rest/api/3/issue").build())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CreateIssueRequest(new Fields(new Project(projectKey), title, new IssueType(type == null || type.isBlank() ? "Task" : type))))
                    .retrieve()
                    .body(JsonNode.class);
            String key = response.path("key").asText();
            return new PublishedIssue(response.path("id").asText(), key, baseUrl.replaceAll("/$", "") + "/browse/" + key);
        } catch (RestClientException exception) {
            throw map("Jira", exception);
        }
    }

    private record CreateIssueRequest(Fields fields) {}
    private record Fields(Project project, String summary, IssueType issuetype) {}
    private record Project(String key) {}
    private record IssueType(String name) {}
    public record ExternalIssue(String id, String key, String title, String description) {}
    public record PublishedIssue(String id, String key, String url) {}
}
