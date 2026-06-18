package com.taskmind.backend.ai;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmind.ai.contracts.AiCapabilityId;
import com.taskmind.ai.contracts.AiProviderId;
import com.taskmind.ai.contracts.AiRunStatus;
import com.taskmind.ai.contracts.audit.AiRunSummary;
import com.taskmind.ai.contracts.capability.CapabilitiesResponse;
import com.taskmind.ai.contracts.capability.CapabilityDescriptor;
import com.taskmind.ai.contracts.chat.ChatRequest;
import com.taskmind.ai.contracts.chat.ChatResponse;
import com.taskmind.backend.security.AuthenticatedUserResolver;
import com.taskmind.backend.security.JwtClaimAuthenticationConverter;
import com.taskmind.backend.security.SecurityConfig;
import com.taskmind.backend.security.TestJwtSupport;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = NovaFacadeController.class)
@Import({
    SecurityConfig.class,
    AuthenticatedUserResolver.class,
    JwtClaimAuthenticationConverter.class,
    TestJwtSupport.Config.class
})
class NovaFacadeControllerTest {
    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired private MockMvc mockMvc;

    @MockBean private NovaClient novaClient;

    @Test
    void novaFacadeRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/v1/nova/capabilities")).andExpect(status().isUnauthorized());
    }

    @Test
    void chatReturnsDeterministicStubbedNovaResponse() throws Exception {
        UUID runId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        when(novaClient.chat(any(ChatRequest.class)))
                .thenReturn(new ChatResponse("session-1", "mock assistant response", runId, List.of()));

        mockMvc.perform(
                        post("/v1/nova/chat")
                                .with(jwt(USER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "message": "Plan my day",
                                          "timezone": "UTC",
                                          "locale": "en-US",
                                          "correlationId": "corr-1"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("session-1"))
                .andExpect(jsonPath("$.message").value("mock assistant response"))
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.actions.length()").value(0));

        verify(novaClient).chat(any(ChatRequest.class));
    }

    @Test
    void chatStreamReturnsAuthenticatedSseChunks() throws Exception {
        doAnswer(invocation -> {
                    var outputStream = (java.io.OutputStream) invocation.getArgument(1);
                    outputStream.write(
                            "data: {\"sessionId\":\"session-1\",\"content\":\"hello\",\"done\":false}\n\n"
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    outputStream.write(
                            "data: {\"sessionId\":\"session-1\",\"content\":\"\",\"done\":true}\n\n"
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    return null;
                })
                .when(novaClient)
                .chatStream(any(ChatRequest.class), any(java.io.OutputStream.class));

        mockMvc.perform(
                        post("/v1/nova/chat/stream")
                                .with(jwt(USER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.TEXT_EVENT_STREAM)
                                .content(
                                        """
                                        {
                                          "message": "Plan my day",
                                          "timezone": "UTC",
                                          "locale": "en-US"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("session-1")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("hello")));

        verify(novaClient).chatStream(any(ChatRequest.class), any(java.io.OutputStream.class));
    }

    @Test
    void chatStreamMapsNovaFailuresToStableProblemDetails() throws Exception {
        doThrow(
                        new NovaClientException(
                                HttpStatus.BAD_GATEWAY,
                                "NOVA_UNAVAILABLE",
                                "Nova service is unavailable"))
                .when(novaClient)
                .chatStream(any(ChatRequest.class), any(java.io.OutputStream.class));

        mockMvc.perform(
                        post("/v1/nova/chat/stream")
                                .with(jwt(USER_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.TEXT_EVENT_STREAM)
                                .content("{\"message\":\"Plan my day\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("AI service unavailable"))
                .andExpect(jsonPath("$.detail").value("Nova service is unavailable"))
                .andExpect(jsonPath("$.code").value("NOVA_UNAVAILABLE"));
    }

    @Test
    void capabilitiesReturnsStubbedContractResponse() throws Exception {
        when(novaClient.capabilities())
                .thenReturn(
                        new CapabilitiesResponse(
                                List.of(
                                        new CapabilityDescriptor(
                                                "capture", "Capture tasks", null, null))));

        mockMvc.perform(get("/v1/nova/capabilities").with(jwt(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capabilities[0].id").value("capture"))
                .andExpect(jsonPath("$.capabilities[0].description").value("Capture tasks"));
    }

    @Test
    void runReturnsStubbedContractResponse() throws Exception {
        UUID runId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        when(novaClient.run(runId))
                .thenReturn(
                        new AiRunSummary(
                                runId,
                                AiRunStatus.SUCCEEDED,
                                AiProviderId.MOCK,
                                AiCapabilityId.CHAT,
                                "mock-model",
                                "corr-1",
                                createdAt,
                                createdAt,
                                createdAt));

        mockMvc.perform(get("/v1/nova/runs/{runId}", runId).with(jwt(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(runId.toString()))
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.providerId").value("mock"));
    }

    @Test
    void mapsNovaFailuresToStableProblemDetails() throws Exception {
        when(novaClient.capabilities())
                .thenThrow(
                        new NovaClientException(
                                HttpStatus.BAD_GATEWAY,
                                "NOVA_UNAVAILABLE",
                                "Nova service is unavailable"));

        mockMvc.perform(get("/v1/nova/capabilities").with(jwt(USER_ID)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("AI service unavailable"))
                .andExpect(jsonPath("$.detail").value("Nova service is unavailable"))
                .andExpect(jsonPath("$.code").value("NOVA_UNAVAILABLE"));
    }
}
