package com.taskmind.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalAiControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void capabilityExecutionWritesAuditRowAndRunCanBeFetched() throws Exception {
        String body =
                objectMapper.writeValueAsString(
                        Map.of(
                                "userId",
                                UUID.randomUUID().toString(),
                                "workspaceId",
                                "workspace-a",
                                "input",
                                Map.of("text", "organize my inbox"),
                                "correlationId",
                                "corr-capability"));

        MvcResult result =
                mockMvc.perform(
                                post("/internal/ai/capabilities/capture:run")
                                        .header("X-Service-Token", "test-ai-token")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                        .andExpect(jsonPath("$.output.provider").value("mock"))
                        .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID runId = UUID.fromString(json.get("runId").asText());
        Integer rows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM ai.ai_runs WHERE id = ? AND status = 'SUCCEEDED'",
                        Integer.class,
                        runId);
        assertThat(rows).isEqualTo(1);

        mockMvc.perform(
                        get("/internal/ai/runs/" + runId)
                                .header("X-Service-Token", "test-ai-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.capabilityId").value("capture"));
    }

    @Test
    void unknownCapabilityIsRejected() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("input", Map.of("text", "x")));

        mockMvc.perform(
                        post("/internal/ai/capabilities/not-real:run")
                                .header("X-Service-Token", "test-ai-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("UNKNOWN_CAPABILITY"));
    }

    @Test
    void chatCreatesAndContinuesSessionWithAuditRows() throws Exception {
        String firstBody =
                objectMapper.writeValueAsString(
                        Map.of("message", "Help me plan today", "correlationId", "corr-chat-1"));
        MvcResult first =
                mockMvc.perform(
                                post("/internal/ai/chat")
                                        .header("X-Service-Token", "test-ai-token")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(firstBody))
                        .andExpect(status().isOk())
                        .andExpect(
                                jsonPath("$.message")
                                        .value(
                                                org.hamcrest.Matchers.startsWith(
                                                        "Mock assistant response")))
                        .andReturn();
        JsonNode firstJson = objectMapper.readTree(first.getResponse().getContentAsString());
        String sessionId = firstJson.get("sessionId").asText();

        String secondBody =
                objectMapper.writeValueAsString(
                        Map.of(
                                "sessionId",
                                sessionId,
                                "message",
                                "Continue",
                                "correlationId",
                                "corr-chat-2"));
        mockMvc.perform(
                        post("/internal/ai/chat")
                                .header("X-Service-Token", "test-ai-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(secondBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId));

        Integer rows =
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM ai.ai_runs WHERE capability_id = 'chat' AND status = 'SUCCEEDED'",
                        Integer.class);
        assertThat(rows).isGreaterThanOrEqualTo(2);
    }
}
