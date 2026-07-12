package com.taskmind.ai.contracts;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.taskmind.ai.contracts.audit.AiRunSummary;
import com.taskmind.ai.contracts.capability.CapabilityRequest;
import com.taskmind.ai.contracts.capability.CapabilityResponse;
import com.taskmind.ai.contracts.chat.ChatAction;
import com.taskmind.ai.contracts.chat.ChatRequest;
import com.taskmind.ai.contracts.chat.ChatResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiContractsSerializationTest {
    private final ObjectMapper objectMapper =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void chatContractsSerializeDeterministicallyAndRoundTrip() throws Exception {
        UUID runId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        ChatRequest request =
                new ChatRequest(
                        "session-1",
                        "Plan my day",
                        "America/New_York",
                        "en-US",
                        "corr-1",
                        "project-1",
                        "task-1",
                        "task");
        ChatResponse response =
                new ChatResponse(
                        "session-1",
                        "Here is a deterministic plan.",
                        runId,
                        List.of(
                                new ChatAction(
                                        "proposal",
                                        objectMapper.readTree(
                                                "{\"kind\":\"task-plan\",\"count\":2}"))));

        String requestJson = objectMapper.writeValueAsString(request);
        String responseJson = objectMapper.writeValueAsString(response);

        assertThat(requestJson)
                .isEqualTo(
                        "{\"sessionId\":\"session-1\",\"message\":\"Plan my day\",\"timezone\":\"America/New_York\",\"locale\":\"en-US\",\"correlationId\":\"corr-1\",\"projectId\":\"project-1\",\"taskId\":\"task-1\",\"scope\":\"task\"}");
        assertThat(responseJson)
                .isEqualTo(
                        "{\"sessionId\":\"session-1\",\"message\":\"Here is a deterministic plan.\",\"runId\":\"00000000-0000-0000-0000-000000000001\",\"actions\":[{\"type\":\"proposal\",\"payload\":{\"kind\":\"task-plan\",\"count\":2}}]}");
        assertThat(objectMapper.readValue(requestJson, ChatRequest.class)).isEqualTo(request);
        assertThat(objectMapper.readValue(responseJson, ChatResponse.class)).isEqualTo(response);
    }

    @Test
    void capabilityContractsSerializeDeterministicallyAndRoundTrip() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID runId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        JsonNode input = objectMapper.readTree("{\"text\":\"turn this into tasks\"}");
        JsonNode output = objectMapper.readTree("{\"items\":[{\"title\":\"First task\"}]}");
        CapabilityRequest request =
                new CapabilityRequest(
                        new AiCapabilityId("capture"),
                        userId,
                        "workspace-1",
                        input,
                        "corr-2",
                        "idem-1");
        CapabilityResponse response =
                new CapabilityResponse(runId, AiRunStatus.SUCCEEDED, output, List.of("mock-provider"), null);

        String requestJson = objectMapper.writeValueAsString(request);
        String responseJson = objectMapper.writeValueAsString(response);

        assertThat(requestJson)
                .isEqualTo(
                        "{\"capabilityId\":\"capture\",\"userId\":\"00000000-0000-0000-0000-000000000010\",\"workspaceId\":\"workspace-1\",\"input\":{\"text\":\"turn this into tasks\"},\"correlationId\":\"corr-2\",\"idempotencyKey\":\"idem-1\"}");
        assertThat(responseJson)
                .isEqualTo(
                        "{\"runId\":\"00000000-0000-0000-0000-000000000011\",\"status\":\"SUCCEEDED\",\"output\":{\"items\":[{\"title\":\"First task\"}]},\"warnings\":[\"mock-provider\"],\"error\":null}");
        assertThat(objectMapper.readValue(requestJson, CapabilityRequest.class)).isEqualTo(request);
        assertThat(objectMapper.readValue(responseJson, CapabilityResponse.class)).isEqualTo(response);
    }

    @Test
    void auditSummarySerializesProviderAndCapabilityIdsAsStableStrings() throws Exception {
        AiRunSummary summary =
                new AiRunSummary(
                        UUID.fromString("00000000-0000-0000-0000-000000000021"),
                        AiRunStatus.PENDING,
                        new AiProviderId("MOCK"),
                        new AiCapabilityId("Weekly-Review"),
                        "nova-mock-v1",
                        "corr-3",
                        Instant.parse("2026-01-02T03:04:05Z"),
                        null,
                        null);

        String json = objectMapper.writeValueAsString(summary);

        assertThat(json)
                .isEqualTo(
                        "{\"runId\":\"00000000-0000-0000-0000-000000000021\",\"status\":\"PENDING\",\"providerId\":\"mock\",\"capabilityId\":\"weekly-review\",\"modelId\":\"nova-mock-v1\",\"correlationId\":\"corr-3\",\"createdAt\":\"2026-01-02T03:04:05Z\",\"startedAt\":null,\"completedAt\":null}");
        assertThat(objectMapper.readValue(json, AiRunSummary.class)).isEqualTo(summary);
    }
}
