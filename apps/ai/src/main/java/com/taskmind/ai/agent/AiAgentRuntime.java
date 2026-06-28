package com.taskmind.ai.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.audit.AiRunAuditRepository;
import com.taskmind.ai.audit.AiRunRecord;
import com.taskmind.ai.capability.Capability;
import com.taskmind.ai.capability.CapabilityRegistry;
import com.taskmind.ai.contracts.AiCapabilityId;
import com.taskmind.ai.contracts.AiRunStatus;
import com.taskmind.ai.contracts.capability.CapabilityError;
import com.taskmind.ai.contracts.capability.CapabilityRequest;
import com.taskmind.ai.contracts.capability.CapabilityResponse;
import com.taskmind.ai.provider.AiProvider;
import com.taskmind.ai.provider.ProviderRequest;
import com.taskmind.ai.provider.ProviderResponse;
import com.taskmind.ai.provider.ProviderRouter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiAgentRuntime {
    private final CapabilityRegistry capabilityRegistry;
    private final ProviderRouter providerRouter;
    private final AiRunAuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public AiAgentRuntime(
            CapabilityRegistry capabilityRegistry,
            ProviderRouter providerRouter,
            AiRunAuditRepository auditRepository,
            ObjectMapper objectMapper) {
        this.capabilityRegistry = capabilityRegistry;
        this.providerRouter = providerRouter;
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    public CapabilityResponse execute(CapabilityRequest request) {
        AiCapabilityId capabilityId = request.capabilityId();
        Capability capability =
                capabilityRegistry
                        .find(capabilityId)
                        .orElseThrow(() -> new UnknownCapabilityException(capabilityId.value()));
        if (request.input() != null && !request.input().isObject()) {
            throw new InvalidCapabilityInputException("Capability input must be a JSON object");
        }
        AiProvider provider = providerRouter.defaultProvider();
        JsonNode providerInput = capability.buildProviderInput(request.input());
        UUID runId =
                auditRepository.start(
                        new AiRunRecord(
                                request.userId(),
                                request.workspaceId(),
                                capability.id(),
                                provider.id(),
                                provider.modelId(),
                                requestHash(capability.id().value(), providerInput),
                                providerInput,
                                promptVersion(providerInput),
                                "VALID",
                                request.correlationId()));
        Instant started = Instant.now();
        try {
            ProviderResponse providerResponse =
                    provider.complete(
                            new ProviderRequest(
                                    capability.id(),
                                    providerInput,
                                    promptVersion(providerInput),
                                    List.of(),
                                    request.correlationId()));
            auditRepository.succeed(
                    runId,
                    providerResponse.output(),
                    providerResponse.promptTokens(),
                    providerResponse.completionTokens(),
                    providerResponse.totalTokens(),
                    elapsed(started, providerResponse.latencyMs()));
            return new CapabilityResponse(
                    runId, AiRunStatus.SUCCEEDED, providerResponse.output(), List.of(), null);
        } catch (RuntimeException ex) {
            auditRepository.fail(runId, "PROVIDER_ERROR", ex.getMessage(), elapsed(started, 0L));
            return new CapabilityResponse(
                    runId,
                    AiRunStatus.FAILED,
                    objectMapper.createObjectNode(),
                    List.of(),
                    new CapabilityError("PROVIDER_ERROR", "Provider execution failed", null));
        }
    }

    private String promptVersion(JsonNode input) {
        JsonNode value = input == null ? null : input.get("promptVersion");
        return value == null || value.asText("").isBlank() ? "default.v1" : value.asText();
    }

    private long elapsed(Instant started, long providerLatencyMs) {
        return Math.max(providerLatencyMs, Duration.between(started, Instant.now()).toMillis());
    }

    private String requestHash(String capabilityId, JsonNode input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash =
                    digest.digest((capabilityId + ":" + input).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
