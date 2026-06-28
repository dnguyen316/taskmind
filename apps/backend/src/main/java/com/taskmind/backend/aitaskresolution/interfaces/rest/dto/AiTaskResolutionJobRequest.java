package com.taskmind.backend.aitaskresolution.interfaces.rest.dto;

import java.util.UUID;

public record AiTaskResolutionJobRequest(UUID templateId, UUID githubProjectLinkId, String idempotencyKey) {}
