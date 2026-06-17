package com.taskmind.backend.integration.infrastructure.github;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GitHubClient {
    public List<ExternalIssue> importIssues(String repository, int limit) {
        int count = Math.min(Math.max(limit, 0), 2);
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(i -> new ExternalIssue("github-" + repository + "-" + i, "#" + i, "Imported GitHub issue " + i, "Stub GitHub import"))
                .toList();
    }
    public record ExternalIssue(String id, String key, String title, String description) {}
}
