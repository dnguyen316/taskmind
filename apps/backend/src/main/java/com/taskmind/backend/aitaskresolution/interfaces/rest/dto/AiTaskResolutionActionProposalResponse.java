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
    public static AiTaskResolutionActionProposalResponse from(AiTaskResolutionActionProposal proposal) {
        return new AiTaskResolutionActionProposalResponse(
                proposal.id(),
                proposal.jobId(),
                proposal.proposedActionType(),
                proposal.payloadPreview(),
                proposal.riskLevel(),
                proposal.rationale(),
                proposal.status(),
                proposal.decidedBy(),
                proposal.decidedAt(),
                proposal.errorCode(),
                proposal.createdAt(),
                proposal.updatedAt());
    }
}
