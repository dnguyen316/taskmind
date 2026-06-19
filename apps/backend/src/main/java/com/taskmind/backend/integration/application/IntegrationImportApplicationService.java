package com.taskmind.backend.integration.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.integration.domain.model.*;
import com.taskmind.backend.integration.domain.repository.IntegrationImportRunRepository;
import com.taskmind.backend.integration.infrastructure.github.GitHubClient;
import com.taskmind.backend.integration.infrastructure.jira.JiraCloudClient;
import com.taskmind.backend.task.application.*;
import com.taskmind.backend.task.domain.model.*;
import com.taskmind.backend.task.domain.model.Task;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationImportApplicationService {
    private final IntegrationProjectLinkApplicationService links; private final IntegrationConnectionApplicationService connections; private final IntegrationImportRunRepository runs; private final IntegrationExternalLinkApplicationService externalLinks; private final TaskApplicationService tasks; private final JiraCloudClient jira; private final GitHubClient github; private final int maxLimit;
    public IntegrationImportApplicationService(IntegrationProjectLinkApplicationService links, IntegrationConnectionApplicationService connections, IntegrationImportRunRepository runs, IntegrationExternalLinkApplicationService externalLinks, TaskApplicationService tasks, JiraCloudClient jira, GitHubClient github, @Value("${taskmind.integrations.import-limit:500}") int maxLimit) { this.links = links; this.connections = connections; this.runs = runs; this.externalLinks = externalLinks; this.tasks = tasks; this.jira = jira; this.github = github; this.maxLimit = maxLimit; }
    @Transactional
    public IntegrationImportRun importIssues(AuthenticatedUser actor, UUID projectLinkId, int requestedLimit) {
        IntegrationProjectLink link = links.requireAccessible(actor, projectLinkId); IntegrationConnection connection = connections.requireOwned(actor, link.connectionId()); IntegrationConnectionApplicationService.ConnectionCredentials credentials = connections.credentials(connection); int limit = Math.min(requestedLimit <= 0 ? maxLimit : requestedLimit, maxLimit); int count = 0;
        if (link.provider() == IntegrationProvider.JIRA) { for (JiraCloudClient.ExternalIssue issue : jira.importIssues(credentials.baseUrl(), credentials.accessToken(), link.externalProjectKey(), limit)) { Task t = tasks.create(actor, new CreateTaskCommand(actor.userId(), link.projectId(), issue.title(), issue.description(), TaskStatus.TODO, 3, null, null, EnergyLevel.MEDIUM, TaskSource.MANUAL, BigDecimal.ONE)); externalLinks.record(t.id(), link.projectId(), link.provider(), "ISSUE", issue.id(), issue.key(), null, "IMPORTED", null); count++; } }
        else if (link.provider() == IntegrationProvider.GITHUB) { for (GitHubClient.ExternalIssue issue : github.importIssues(credentials.baseUrl(), credentials.accessToken(), link.externalProjectId(), limit)) { Task t = tasks.create(actor, new CreateTaskCommand(actor.userId(), link.projectId(), issue.title(), issue.description(), TaskStatus.TODO, 3, null, null, EnergyLevel.MEDIUM, TaskSource.MANUAL, BigDecimal.ONE)); externalLinks.record(t.id(), link.projectId(), link.provider(), "ISSUE", issue.id(), issue.key(), null, "IMPORTED", null); count++; } }
        else throw new IllegalArgumentException("Provider does not support issue import");
        Instant now = Instant.now(); return runs.save(new IntegrationImportRun(UUID.randomUUID(), null, link.projectId(), link.id(), link.provider(), "COMPLETED", count, 0, null, actor.userId(), now, now));
    }
}
