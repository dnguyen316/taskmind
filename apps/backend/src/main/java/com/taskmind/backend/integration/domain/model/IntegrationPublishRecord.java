package com.taskmind.backend.integration.domain.model;

import java.time.Instant;
import java.util.UUID;

public record IntegrationPublishRecord(UUID id, Long version, UUID taskId, UUID projectLinkId, IntegrationProvider provider, String externalId, String externalKey, String externalUrl, String status, UUID publishedBy, Instant publishedAt, String metadataJson) {}
