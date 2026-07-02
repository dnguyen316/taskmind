package com.taskmind.backend.aiproposal.interfaces.rest.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record AiActionProposalDecisionRequest(String reason, JsonNode editedPayload) {}
