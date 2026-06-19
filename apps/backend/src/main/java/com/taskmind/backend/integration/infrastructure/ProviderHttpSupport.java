package com.taskmind.backend.integration.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public final class ProviderHttpSupport {
    private ProviderHttpSupport() {}

    public static ProviderClientException map(String provider, RestClientException exception) {
        if (exception instanceof RestClientResponseException response) {
            HttpStatusCode status = response.getStatusCode();
            String code = codeFor(status);
            boolean retrySafe = status.value() == 408 || status.value() == 429 || status.is5xxServerError();
            String message = provider + " request failed with HTTP " + status.value();
            return new ProviderClientException(status, code, message, retrySafe);
        }
        return new ProviderClientException(HttpStatus.SERVICE_UNAVAILABLE, "PROVIDER_UNAVAILABLE", provider + " request failed", true);
    }

    private static String codeFor(HttpStatusCode status) {
        if (status.value() == 401 || status.value() == 403) return "PROVIDER_AUTH_FAILED";
        if (status.value() == 429) return "PROVIDER_RATE_LIMITED";
        if (status.is4xxClientError()) return "PROVIDER_REJECTED_REQUEST";
        if (status.is5xxServerError()) return "PROVIDER_UNAVAILABLE";
        return "PROVIDER_ERROR";
    }

    public static String text(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isTextual()) return node.asText();
        JsonNode content = node.path("content");
        if (content.isArray()) {
            StringBuilder out = new StringBuilder();
            appendText(content, out);
            return out.toString().trim();
        }
        return node.toString();
    }

    private static void appendText(JsonNode node, StringBuilder out) {
        if (node == null || node.isNull()) return;
        if (node.isArray()) node.forEach(child -> appendText(child, out));
        else if (node.isObject()) {
            if (node.path("type").asText().equals("text")) out.append(node.path("text").asText()).append(' ');
            appendText(node.path("content"), out);
        }
    }
}
