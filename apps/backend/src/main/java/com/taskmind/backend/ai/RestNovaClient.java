package com.taskmind.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.audit.AiRunSummary;
import com.taskmind.ai.contracts.capability.CapabilitiesResponse;
import com.taskmind.ai.contracts.capability.CapabilityError;
import com.taskmind.ai.contracts.capability.CapabilityRequest;
import com.taskmind.ai.contracts.capability.CapabilityResponse;
import com.taskmind.ai.contracts.chat.ChatRequest;
import com.taskmind.ai.contracts.chat.ChatResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestNovaClient implements NovaClient {
    static final String SERVICE_TOKEN_HEADER = "X-Service-Token";

    private final RestClient restClient;
    private final NovaClientProperties properties;
    private final ObjectMapper objectMapper;

    public RestNovaClient(
            RestClient novaRestClient, NovaClientProperties properties, ObjectMapper objectMapper) {
        this.restClient = novaRestClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        return exchange(
                () ->
                        restClient
                                .post()
                                .uri("/internal/ai/chat")
                                .header(SERVICE_TOKEN_HEADER, properties.serviceToken())
                                .body(request)
                                .retrieve()
                                .body(ChatResponse.class));
    }

    @Override
    public CapabilityResponse executeCapability(String capabilityId, CapabilityRequest request) {
        return exchange(
                () ->
                        restClient
                                .post()
                                .uri("/internal/ai/capabilities/{capabilityId}:run", capabilityId)
                                .header(SERVICE_TOKEN_HEADER, properties.serviceToken())
                                .body(request)
                                .retrieve()
                                .body(CapabilityResponse.class));
    }

    @Override
    public CapabilitiesResponse capabilities() {
        return exchange(
                () ->
                        restClient
                                .get()
                                .uri("/internal/ai/capabilities")
                                .header(SERVICE_TOKEN_HEADER, properties.serviceToken())
                                .retrieve()
                                .body(CapabilitiesResponse.class));
    }

    @Override
    public AiRunSummary run(UUID runId) {
        return exchange(
                () ->
                        restClient
                                .get()
                                .uri("/internal/ai/runs/{runId}", runId)
                                .header(SERVICE_TOKEN_HEADER, properties.serviceToken())
                                .retrieve()
                                .body(AiRunSummary.class));
    }

    private <T> T exchange(NovaExchange<T> exchange) {
        try {
            return exchange.execute();
        } catch (HttpStatusCodeException exception) {
            throw mapStatusException(exception);
        } catch (RestClientException exception) {
            throw new NovaClientException(
                    HttpStatus.BAD_GATEWAY,
                    "NOVA_UNAVAILABLE",
                    "Nova service is unavailable");
        }
    }

    private NovaClientException mapStatusException(HttpStatusCodeException exception) {
        CapabilityError error = readCapabilityError(exception);
        HttpStatusCode clientStatus = facadeStatus(exception.getStatusCode());
        String code = error != null && error.code() != null ? error.code() : "NOVA_REQUEST_FAILED";
        String message =
                error != null && error.message() != null
                        ? error.message()
                        : "Nova request failed";
        return new NovaClientException(clientStatus, code, message);
    }

    private HttpStatusCode facadeStatus(HttpStatusCode novaStatus) {
        if (novaStatus.isSameCodeAs(HttpStatus.BAD_REQUEST)) {
            return HttpStatus.BAD_REQUEST;
        }
        if (novaStatus.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_GATEWAY;
    }

    private CapabilityError readCapabilityError(HttpStatusCodeException exception) {
        byte[] bytes = exception.getResponseBodyAsByteArray();
        if (bytes.length == 0) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(new String(bytes, StandardCharsets.UTF_8));
            if (node.has("code")) {
                return objectMapper.treeToValue(node, CapabilityError.class);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    @FunctionalInterface
    private interface NovaExchange<T> {
        T execute();
    }
}
