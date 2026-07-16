package com.taskmind.backend.integration.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.integration.domain.model.IntegrationConnection;
import com.taskmind.backend.integration.domain.model.IntegrationImportRun;
import com.taskmind.backend.integration.domain.model.IntegrationProjectLink;
import com.taskmind.backend.integration.domain.model.IntegrationProvider;
import com.taskmind.backend.integration.domain.repository.IntegrationImportRunRepository;
import com.taskmind.backend.integration.infrastructure.github.GitHubClient;
import com.taskmind.backend.integration.infrastructure.jira.JiraCloudClient;
import com.taskmind.backend.task.application.CreateTaskCommand;
import com.taskmind.backend.task.application.TaskApplicationService;
import com.taskmind.backend.task.domain.model.EnergyLevel;
import com.taskmind.backend.task.domain.model.Task;
import com.taskmind.backend.task.domain.model.TaskSource;
import com.taskmind.backend.task.domain.model.TaskStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationImportApplicationService {
    private final IntegrationProjectLinkApplicationService links;
    private final IntegrationConnectionApplicationService connections;
    private final IntegrationImportRunRepository runs;
    private final IntegrationExternalLinkApplicationService externalLinks;
    private final TaskApplicationService tasks;
    private final JiraCloudClient jira;
    private final GitHubClient github;
    private final int maxLimit;

    public IntegrationImportApplicationService(
            IntegrationProjectLinkApplicationService links,
            IntegrationConnectionApplicationService connections,
            IntegrationImportRunRepository runs,
            IntegrationExternalLinkApplicationService externalLinks,
            TaskApplicationService tasks,
            JiraCloudClient jira,
            GitHubClient github,
            @Value("${taskmind.integrations.import-limit:500}") int maxLimit) {
        this.links = links;
        this.connections = connections;
        this.runs = runs;
        this.externalLinks = externalLinks;
        this.tasks = tasks;
        this.jira = jira;
        this.github = github;
        this.maxLimit = maxLimit;
    }

    @Transactional
    public IntegrationImportRun importIssues(AuthenticatedUser actor, UUID projectLinkId, int requestedLimit) {
        IntegrationProjectLink link = links.requireAccessible(actor, projectLinkId);
        IntegrationConnection connection = connections.requireOwned(actor, link.connectionId());
        IntegrationConnectionApplicationService.ConnectionCredentials credentials = connections.credentials(connection);
        int limit = Math.min(requestedLimit <= 0 ? maxLimit : requestedLimit, maxLimit);
        ImportCounts counts;
        if (link.provider() == IntegrationProvider.JIRA) {
            counts = importJiraIssues(actor, link, credentials, limit);
        } else if (link.provider() == IntegrationProvider.GITHUB) {
            counts = importGitHubIssues(actor, link, credentials, limit);
        } else {
            throw new IllegalArgumentException("Provider does not support issue import");
        }
        Instant now = Instant.now();
        return runs.save(new IntegrationImportRun(
                UUID.randomUUID(),
                null,
                link.projectId(),
                link.id(),
                link.provider(),
                "COMPLETED",
                counts.importedCount(),
                counts.skippedCount(),
                null,
                actor.userId(),
                now,
                now));
    }

    private ImportCounts importJiraIssues(
            AuthenticatedUser actor,
            IntegrationProjectLink link,
            IntegrationConnectionApplicationService.ConnectionCredentials credentials,
            int limit) {
        ImportCounts counts = new ImportCounts();
        for (JiraCloudClient.ExternalIssue issue : jira.importIssues(
                credentials.baseUrl(), credentials.accessToken(), link.externalProjectKey(), limit)) {
            importIssue(actor, link, issue.id(), issue.key(), issue.title(), issue.description(), counts);
        }
        return counts;
    }

    private ImportCounts importGitHubIssues(
            AuthenticatedUser actor,
            IntegrationProjectLink link,
            IntegrationConnectionApplicationService.ConnectionCredentials credentials,
            int limit) {
        ImportCounts counts = new ImportCounts();
        for (GitHubClient.ExternalIssue issue : github.importIssues(
                credentials.baseUrl(), credentials.accessToken(), link.externalProjectId(), limit)) {
            importIssue(actor, link, issue.id(), issue.key(), issue.title(), issue.description(), counts);
        }
        return counts;
    }

    private void importIssue(
            AuthenticatedUser actor,
            IntegrationProjectLink link,
            String externalId,
            String externalKey,
            String title,
            String description,
            ImportCounts counts) {
        if (externalLinks.findExistingExternalIssue(link.provider(), externalId, externalKey).isPresent()) {
            counts.skippedCount++;
            return;
        }
        Task task = tasks.create(actor, new CreateTaskCommand(
                actor.userId(),
                link.projectId(),
                title,
                description,
                TaskStatus.TODO,
                3,
                null,
                null,
                EnergyLevel.MEDIUM,
                TaskSource.MANUAL,
                BigDecimal.ONE));
        externalLinks.record(
                task.id(), link.projectId(), link.provider(), "ISSUE", externalId, externalKey, null, "IMPORTED", null);
        counts.importedCount++;
    }

    private static final class ImportCounts {
        private int importedCount;
        private int skippedCount;

        private int importedCount() {
            return importedCount;
        }

        private int skippedCount() {
            return skippedCount;
        }
    }
}
