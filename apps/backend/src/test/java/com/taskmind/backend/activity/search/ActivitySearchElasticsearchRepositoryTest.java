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

        var suggestions = repository.suggest(new ActivitySearchRequest(USER_ID, "tas", 5, null, null, null, null, null, null));

        assertThat(suggestions).containsExactly("Task planning", "task.created", "task.updated");
        server.verify();
    }

    @Test
    void recommendsTypedActivityHitsForAuthenticatedUser() {
        server.expect(once(), requestTo(BASE_URL + "/" + INDEX_NAME + "/_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content()
                                .string(
                                        allOf(
                                                containsString("\"userId\":\"" + USER_ID + "\""),
                                                containsString("\"query\":\"tas\""),
                                                containsString("\"type\":\"bool_prefix\""),
                                                containsString("entityType"),
                                                containsString("entityId"),
                                                containsString("occurredAt"))))
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "hits": {
                                    "hits": [
                                      {"_source": {"entityType": "task", "entityId": "33333333-3333-3333-3333-333333333333", "title": "Task planning", "eventType": "task.updated", "status": "DONE", "occurredAt": "2026-01-02T00:00:00Z"}}
                                    ]
                                  }
                                }
                                """,
                                MediaType.APPLICATION_JSON));

        var recommendations =
                repository.recommend(new ActivitySearchRequest(USER_ID, "tas", 5, null, null, null, null, null, null));

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).label()).isEqualTo("Task planning");
        assertThat(recommendations.get(0).entityType()).isEqualTo("task");
        assertThat(recommendations.get(0).entityId())
                .isEqualTo(UUID.fromString("33333333-3333-3333-3333-333333333333"));
        assertThat(recommendations.get(0).status()).isEqualTo("DONE");
        assertThat(recommendations.get(0).routeName()).isEqualTo("task-detail");
        server.verify();
    }

    @Test
    void searchAppliesStructuredFiltersForAuthenticatedUser() {
        server.expect(once(), requestTo(BASE_URL + "/" + INDEX_NAME + "/_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content()
                                .string(
                                        allOf(
                                                containsString("\"userId\":\"" + USER_ID + "\""),
                                                containsString("\"entityType\":\"task\""),
                                                containsString("\"status\":\"DONE\""),
                                                containsString("\"projectId\":\"22222222-2222-2222-2222-222222222222\""),
                                                containsString("\"eventType\":\"task.updated\""),
                                                containsString("\"gte\":\"2026-01-01T00:00:00Z\""),
                                                containsString("\"lte\":\"2026-01-31T00:00:00Z\""))))
                .andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));

        var results =
                repository.search(
                        new ActivitySearchRequest(
                                USER_ID,
                                "plan",
                                20,
                                "task",
                                "DONE",
                                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                                java.time.Instant.parse("2026-01-01T00:00:00Z"),
                                java.time.Instant.parse("2026-01-31T00:00:00Z"),
                                "task.updated"));

        assertThat(results).isEmpty();
        server.verify();
    }

    @Test
    void skipsBlankSuggestionQueries() {
        var blankRequest = new ActivitySearchRequest(USER_ID, " ", 5, null, null, null, null, null, null);

        assertThat(repository.suggest(blankRequest)).isEmpty();
        assertThat(repository.recommend(blankRequest)).isEmpty();
    }
}
