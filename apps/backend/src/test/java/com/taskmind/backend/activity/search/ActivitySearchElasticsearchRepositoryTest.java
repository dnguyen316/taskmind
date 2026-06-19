package com.taskmind.backend.activity.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ActivitySearchElasticsearchRepositoryTest {
    private static final String BASE_URL = "http://search.test";
    private static final String INDEX_NAME = "activity-events";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private MockRestServiceServer server;
    private ActivitySearchElasticsearchConfig.ElasticsearchActivitySearchRepository repository;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        repository =
                new ActivitySearchElasticsearchConfig.ElasticsearchActivitySearchRepository(
                        builder.build(), new ObjectMapper(), INDEX_NAME);
    }

    @Test
    void suggestsMatchingActivityFieldsForAuthenticatedUser() {
        server.expect(once(), requestTo(BASE_URL + "/" + INDEX_NAME + "/_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content()
                                .string(
                                        allOf(
                                                containsString("\"userId\":\"" + USER_ID + "\""),
                                                containsString("\"query\":\"tas\""),
                                                containsString("\"type\":\"bool_prefix\""),
                                                containsString("title^3"),
                                                containsString("eventType"),
                                                containsString("status"),
                                                containsString("payloadText"))))
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "hits": {
                                    "hits": [
                                      {"_source": {"title": "Task planning", "eventType": "task.created", "status": "TODO"}},
                                      {"_source": {"title": "Task planning", "eventType": "task.updated", "status": "DONE"}}
                                    ]
                                  }
                                }
                                """,
                                MediaType.APPLICATION_JSON));

        var suggestions = repository.suggest(USER_ID, "tas", 5);

        assertThat(suggestions).containsExactly("Task planning", "task.created", "task.updated");
        server.verify();
    }

    @Test
    void skipsBlankSuggestionQueries() {
        assertThat(repository.suggest(USER_ID, " ", 5)).isEmpty();
    }
}
