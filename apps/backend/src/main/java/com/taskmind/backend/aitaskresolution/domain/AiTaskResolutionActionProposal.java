package com.taskmind.backend.aitaskresolution.domain;

import java.time.Instant;
import java.util.UUID;

public record AiTaskResolutionActionProposal(
        UUID id,
        UUID jobId,
        String proposedActionType,
        String payloadPreview,
        String riskLevel,
        String rationale,
        AiTaskResolutionActionProposalStatus status,
        UUID decidedBy,
        Instant decidedAt,
        String errorCode,
        Instant createdAt,
        Instant updatedAt) {}
