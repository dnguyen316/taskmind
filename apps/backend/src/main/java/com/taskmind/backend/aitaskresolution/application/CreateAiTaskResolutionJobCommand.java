package com.taskmind.backend.aitaskresolution.application;

import java.util.UUID;

public record CreateAiTaskResolutionJobCommand(UUID templateId, UUID githubProjectLinkId, String idempotencyKey) {}
