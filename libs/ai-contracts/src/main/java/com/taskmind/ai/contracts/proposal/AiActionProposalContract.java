package com.taskmind.ai.contracts.proposal;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record AiActionProposalContract(
        UUID id,
        UUID userId,
        AiActionProposalType actionType,
        AiActionProposalStatus status,
        JsonNode proposedPayload,
        String preview,
        String rationale,
        String proposer,
        String provider,
        String model,
        AiActionProposalSource source,
        String sourceContext,
        Instant createdAt,
        Instant expiresAt,
        Instant acceptedAt,
        UUID decidedBy,
        String userDecision) {}
