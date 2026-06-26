package com.taskmind.backend.integration.application;

import com.taskmind.backend.integration.domain.model.*;
import com.taskmind.backend.integration.domain.repository.IntegrationExternalLinkRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class IntegrationExternalLinkApplicationService {
    private final IntegrationExternalLinkRepository links;
    public IntegrationExternalLinkApplicationService(IntegrationExternalLinkRepository links) { this.links = links; }
    public Optional<IntegrationExternalLink> findExistingExternalIssue(IntegrationProvider provider, String externalId, String externalKey) {
        return links.findByProviderAndExternalTypeAndExternalIdentity(provider, "ISSUE", externalId, externalKey);
    }
    public IntegrationExternalLink record(UUID taskId, UUID projectId, IntegrationProvider provider, String type, String externalId, String externalKey, String url, String direction, String metadataJson) {
        return links.findByTaskIdAndProvider(taskId, provider).orElseGet(() -> { Instant now = Instant.now(); return links.save(new IntegrationExternalLink(UUID.randomUUID(), null, taskId, projectId, provider, type, externalId, externalKey, url, direction, metadataJson, null, null, null, null, null, now, now)); });
    }
}
