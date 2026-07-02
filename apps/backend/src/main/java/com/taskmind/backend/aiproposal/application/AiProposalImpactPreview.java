package com.taskmind.backend.aiproposal.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskmind.ai.contracts.proposal.AiActionProposalType;
import java.util.List;
import java.util.UUID;

public record AiProposalImpactPreview(
        UUID proposalId,
        AiActionProposalType actionType,
        String summary,
        List<String> affectedResources,
        JsonNode proposedPayload) {}
