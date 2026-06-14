package com.taskmind.backend.ai.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import com.taskmind.ai.contracts.capability.CapabilityRequest;
import com.taskmind.backend.ai.NovaClient;
import com.taskmind.backend.ai.NovaClientException;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiFacadeApplicationService {
    private final NovaClient novaClient;
    private final AiFacadeLocalFallbacks fallbacks;
    private final AiDomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public AiFacadeApplicationService(
            NovaClient novaClient,
            AiFacadeLocalFallbacks fallbacks,
            AiDomainEventPublisher eventPublisher,
            ObjectMapper objectMapper) {
        this.novaClient = novaClient;
        this.fallbacks = fallbacks;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    public CaptureResult capture(UUID userId, String text) {
        eventPublisher.publish(userId, "ai.capture_submitted", Map.of("length", text.length()));
        return runOrFallback(
                AiCapabilityId.CAPTURE,
                userId,
                Map.of("text", text),
                CaptureResult.class,
                () -> fallbacks.capture(text));
    }

    public DescribeTaskResult describe(UUID userId, String title, String notes) {
        return runOrFallback(
                new AiCapabilityId("describe-task"),
                userId,
                Map.of("title", title, "notes", notes == null ? "" : notes),
                DescribeTaskResult.class,
                () -> fallbacks.describe(title, notes));
    }

    public DescribeTaskAutocompleteResult autocomplete(UUID userId, String text) {
        return runOrFallback(
                new AiCapabilityId("autocomplete-task"),
                userId,
                Map.of("text", text),
                DescribeTaskAutocompleteResult.class,
                () -> fallbacks.autocomplete(text));
    }

    public TranslateTaskResult translate(UUID userId, String text, String targetLanguage) {
        return runOrFallback(
                new AiCapabilityId("translate-task"),
                userId,
                Map.of("text", text, "targetLanguage", targetLanguage),
                TranslateTaskResult.class,
                () -> fallbacks.translate(text, targetLanguage));
    }

    private <T> T runOrFallback(
            AiCapabilityId capabilityId,
            UUID userId,
            Map<String, Object> input,
            Class<T> type,
            Fallback<T> fallback) {
        try {
            JsonNode node = objectMapper.valueToTree(input);
            JsonNode output =
                    novaClient
                            .executeCapability(
                                    capabilityId.value(),
                                    new CapabilityRequest(
                                            capabilityId, userId, "default", node, null, null))
                            .output();
            if (!hasExpectedShape(output, type)) {
                return fallback.get();
            }
            return objectMapper.treeToValue(output, type);
        } catch (NovaClientException | IllegalArgumentException ex) {
            return fallback.get();
        } catch (Exception ex) {
            return fallback.get();
        }
    }

    private boolean hasExpectedShape(JsonNode output, Class<?> type) {
        if (output == null || !output.isObject()) {
            return false;
        }
        if (type.equals(CaptureResult.class)) {
            return output.has("drafts");
        }
        if (type.equals(DescribeTaskResult.class)) {
            return output.has("description");
        }
        if (type.equals(DescribeTaskAutocompleteResult.class)) {
            return output.has("suggestions");
        }
        if (type.equals(TranslateTaskResult.class)) {
            return output.has("translatedText");
        }
        return true;
    }

    @FunctionalInterface
    private interface Fallback<T> {
        T get();
    }
}
