package com.taskmind.backend.aitaskresolution.interfaces.rest.dto;

import com.taskmind.backend.aitaskresolution.domain.*;
import java.time.Instant;
import java.util.UUID;

public record AiTaskResolutionActionProposalResponse(
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
        Instant updatedAt) {
    public static AiTaskResolutionActionProposalResponse from(AiTaskResolutionActionProposal p) {
        return new AiTaskResolutionActionProposalResponse(
                p.id(),
                p.jobId(),
                p.proposedActionType(),
                p.payloadPreview(),
                p.riskLevel(),
                p.rationale(),
                p.status(),
                p.decidedBy(),
                p.decidedAt(),
                p.errorCode(),
                p.createdAt(),
                p.updatedAt());
    }
}
