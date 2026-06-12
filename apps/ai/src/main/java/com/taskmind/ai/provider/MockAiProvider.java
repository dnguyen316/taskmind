package com.taskmind.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskmind.ai.contracts.AiProviderId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MockAiProvider implements AiProvider {
    private final String modelId;
    private final ObjectMapper objectMapper;

    public MockAiProvider(
            @Value("${taskmind.ai.provider.mock.model-id:nova-mock-v1}") String modelId,
            ObjectMapper objectMapper) {
        this.modelId = modelId;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiProviderId id() {
        return AiProviderId.MOCK;
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public ProviderResponse complete(ProviderRequest request) {
        String canonical =
                request.capabilityId().value()
                        + ":"
                        + stableJson(request.input())
                        + ":"
                        + request.messages();
        String fingerprint = sha256(canonical).substring(0, 12);
        ObjectNode output = objectMapper.createObjectNode();
        output.put("provider", id().value());
        output.put("model", modelId);
        output.put("capabilityId", request.capabilityId().value());
        output.put("fingerprint", fingerprint);
        output.put("summary", "Mock result " + fingerprint);
        int promptTokens = Math.max(1, canonical.length() / 4);
        int completionTokens = 8;
        return new ProviderResponse(
                "Mock assistant response " + fingerprint,
                output,
                promptTokens,
                completionTokens,
                promptTokens + completionTokens,
                0L);
    }

    private String stableJson(JsonNode input) {
        if (input == null || input.isNull()) {
            return "null";
        }
        return input.toString();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
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
