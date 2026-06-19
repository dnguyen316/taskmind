package com.taskmind.backend.integration.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.integration.domain.model.*;
import com.taskmind.backend.integration.domain.repository.IntegrationPublishRecordRepository;
import com.taskmind.backend.integration.infrastructure.jira.JiraCloudClient;
import com.taskmind.backend.integration.infrastructure.wiki.WikiClient;
import com.taskmind.backend.task.application.TaskApplicationService;
import com.taskmind.backend.task.domain.model.Task;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationPublishApplicationService {
    private final IntegrationProjectLinkApplicationService links; private final IntegrationConnectionApplicationService connections; private final IntegrationPublishRecordRepository records; private final IntegrationExternalLinkApplicationService externalLinks; private final TaskApplicationService tasks; private final JiraCloudClient jira; private final WikiClient wiki;
    public IntegrationPublishApplicationService(IntegrationProjectLinkApplicationService links, IntegrationConnectionApplicationService connections, IntegrationPublishRecordRepository records, IntegrationExternalLinkApplicationService externalLinks, TaskApplicationService tasks, JiraCloudClient jira, WikiClient wiki) { this.links = links; this.connections = connections; this.records = records; this.externalLinks = externalLinks; this.tasks = tasks; this.jira = jira; this.wiki = wiki; }
    @Transactional
    public IntegrationPublishRecord publish(AuthenticatedUser actor, UUID taskId, UUID projectLinkId) {
        IntegrationProjectLink link = links.requireAccessible(actor, projectLinkId); IntegrationConnection connection = connections.requireOwned(actor, link.connectionId()); IntegrationConnectionApplicationService.ConnectionCredentials credentials = connections.credentials(connection); Task task = tasks.findById(actor, taskId).orElseThrow(() -> new IllegalArgumentException("Task not found"));
        return records.findByTaskIdAndProjectLinkId(taskId, projectLinkId).orElseGet(() -> {
            String id, key, url, type;
            if (link.provider() == IntegrationProvider.JIRA) { JiraCloudClient.PublishedIssue p = jira.publish(credentials.baseUrl(), credentials.accessToken(), link.externalProjectKey(), task.title(), task.taskType().name()); id = p.id(); key = p.key(); url = p.url(); type = "ISSUE"; }
            else if (link.provider() == IntegrationProvider.WIKI) { WikiClient.PublishedPage p = wiki.publish(credentials.baseUrl(), credentials.accessToken(), link.externalProjectKey(), task.title()); id = p.id(); key = p.key(); url = p.url(); type = "PAGE"; }
            else throw new IllegalArgumentException("Provider does not support publish");
            externalLinks.record(taskId, link.projectId(), link.provider(), type, id, key, url, "PUBLISHED", null);
            return records.save(new IntegrationPublishRecord(UUID.randomUUID(), null, taskId, projectLinkId, link.provider(), id, key, url, "PUBLISHED", actor.userId(), Instant.now(), null));
        });
    }
}
