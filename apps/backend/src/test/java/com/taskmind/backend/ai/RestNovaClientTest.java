package com.taskmind.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.chat.ChatRequest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

class RestNovaClientTest {
    private static final String BASE_URL = "http://nova.test";
    private static final String SERVICE_TOKEN = "server-token";

    private MockRestServiceServer server;
    private RestNovaClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client =
                new RestNovaClient(
                        builder.build(),
                        new NovaClientProperties(BASE_URL, SERVICE_TOKEN),
                        new ObjectMapper());
    }

    @Test
    void forwardsServiceTokenOnlyOnServerToServerRequest() {
        UUID runId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        server.expect(once(), requestTo(BASE_URL + "/internal/ai/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(RestNovaClient.SERVICE_TOKEN_HEADER, SERVICE_TOKEN))
                .andRespond(
                        withSuccess(
                                        """
                                        {
                                          "sessionId": "session-1",
                                          "message": "mock response",
                                          "runId": "%s",
                                          "actions": []
                                        }
                                        """
                                                .formatted(runId),
                                        MediaType.APPLICATION_JSON));

        var response = client.chat(new ChatRequest(null, "Hello", "UTC", "en-US", "corr-1"));

        assertThat(response.runId()).isEqualTo(runId);
        assertThat(response.message()).isEqualTo("mock response");
        server.verify();
    }

    @Test
    void mapsNovaCapabilityErrorsToStableClientException() {
        server.expect(once(), requestTo(BASE_URL + "/internal/ai/capabilities"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(RestNovaClient.SERVICE_TOKEN_HEADER, SERVICE_TOKEN))
                .andRespond(
                        withBadRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(
                                        """
                                        {
                                          "code": "INVALID_REQUEST",
                                          "message": "Capability input is invalid",
                                          "details": null
                                        }
                                        """));

        assertThatThrownBy(() -> client.capabilities())
                .isInstanceOf(NovaClientException.class)
                .extracting("statusCode", "errorCode", "message")
                .containsExactly(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "INVALID_REQUEST",
                        "Capability input is invalid");
        server.verify();
    }
}
