package com.taskmind.backend.integration.infrastructure.jira;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JiraCloudClient {
    public List<ExternalIssue> importIssues(String externalProjectKey, int limit) {
        int count = Math.min(Math.max(limit, 0), 2);
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> new ExternalIssue("jira-" + externalProjectKey + "-" + i, externalProjectKey + "-" + i, "Imported Jira issue " + i, "Stub Jira import"))
                .toList();
    }
    public PublishedIssue publish(String projectKey, String title, String type) {
        String key = (projectKey == null || projectKey.isBlank() ? "TM" : projectKey) + "-" + Math.abs(title.hashCode() % 100000);
        return new PublishedIssue("jira-" + key, key, "https://jira.example.test/browse/" + key);
    }
    public record ExternalIssue(String id, String key, String title, String description) {}
    public record PublishedIssue(String id, String key, String url) {}
}
