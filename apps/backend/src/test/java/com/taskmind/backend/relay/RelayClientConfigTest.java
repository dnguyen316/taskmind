package com.taskmind.backend.relay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RelayClientConfigTest {
    private static final String BASE_URL = "http://relay.test";
    private static final String SERVICE_TOKEN = "configured-relay-token";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockRestServiceServer server;
    private RelayContextClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient =
                new RelayClientConfig()
                        .relayRestClient(
                                builder, new RelayClientProperties(BASE_URL, SERVICE_TOKEN));
        client = new RelayContextClient(restClient);
    }

    @Test
    void relaysContextRequestsWithBearerAuthorizationHeader() {
        server.expect(
                        once(),
                        requestTo(BASE_URL + "/internal/context/users/" + USER_ID + "/tasks"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + SERVICE_TOKEN))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        List<?> tasks = client.userTasks(USER_ID);

        assertThat(tasks).isEmpty();
        server.verify();
    }
}
