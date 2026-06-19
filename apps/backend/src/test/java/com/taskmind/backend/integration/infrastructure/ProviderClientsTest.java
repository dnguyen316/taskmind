package com.taskmind.backend.integration.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.taskmind.backend.integration.infrastructure.github.GitHubClient;
import com.taskmind.backend.integration.infrastructure.jira.JiraCloudClient;
import com.taskmind.backend.integration.infrastructure.wiki.WikiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ProviderClientsTest {
    private static final String BASE_URL = "https://provider.test";
    private MockRestServiceServer server;
    private JiraCloudClient jira;
    private GitHubClient github;
    private WikiClient wiki;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        jira = new JiraCloudClient(builder);
        github = new GitHubClient(builder);
        wiki = new WikiClient(builder);
    }

    @Test
    void importsJiraIssuesWithConnectionBaseUrlAndBearerToken() {
        server.expect(once(), requestTo(BASE_URL + "/rest/api/3/search?jql=project%20%3D%20TM%20ORDER%20BY%20updated%20DESC&maxResults=2&fields=summary,description"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer jira-token"))
                .andRespond(withSuccess("""
                        {"issues":[{"id":"10001","key":"TM-1","fields":{"summary":"Fix auth","description":"Body"}}]}
                        """, MediaType.APPLICATION_JSON));

        var issues = jira.importIssues(BASE_URL, "jira-token", "TM", 2);

        assertThat(issues).singleElement().satisfies(issue -> {
            assertThat(issue.id()).isEqualTo("10001");
            assertThat(issue.key()).isEqualTo("TM-1");
            assertThat(issue.title()).isEqualTo("Fix auth");
        });
        server.verify();
    }

    @Test
    void publishesJiraIssueAndBuildsBrowseUrl() {
        server.expect(once(), requestTo(BASE_URL + "/rest/api/3/issue"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer jira-token"))
                .andRespond(withSuccess("{\"id\":\"10002\",\"key\":\"TM-2\"}", MediaType.APPLICATION_JSON));

        var published = jira.publish(BASE_URL, "jira-token", "TM", "Ship feature", "Task");

        assertThat(published.id()).isEqualTo("10002");
        assertThat(published.url()).isEqualTo(BASE_URL + "/browse/TM-2");
        server.verify();
    }

    @Test
    void importsGitHubIssuesAndSkipsPullRequests() {
        server.expect(once(), requestTo(BASE_URL + "/repos/taskmind/app/issues?state=open&per_page=10"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer gh-token"))
                .andRespond(withSuccess("""
                        [{"id":1,"node_id":"I_1","number":7,"title":"Bug","body":"Details"},{"id":2,"number":8,"title":"PR","pull_request":{}}]
                        """, MediaType.APPLICATION_JSON));

        var issues = github.importIssues(BASE_URL, "gh-token", "taskmind/app", 10);

        assertThat(issues).singleElement().satisfies(issue -> {
            assertThat(issue.id()).isEqualTo("I_1");
            assertThat(issue.key()).isEqualTo("#7");
        });
        server.verify();
    }

    @Test
    void publishesWikiPageWithProviderApiUrl() {
        server.expect(once(), requestTo(BASE_URL + "/wiki/rest/api/content"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer wiki-token"))
                .andRespond(withSuccess("{\"id\":\"abc\",\"_links\":{\"webui\":\"/wiki/spaces/ENG/pages/abc\"}}", MediaType.APPLICATION_JSON));

        var page = wiki.publish(BASE_URL, "wiki-token", "ENG", "Runbook");

        assertThat(page.id()).isEqualTo("abc");
        assertThat(page.url()).isEqualTo(BASE_URL + "/wiki/spaces/ENG/pages/abc");
        server.verify();
    }

    @Test
    void mapsAuthFailuresProviderErrorsAndRateLimits() {
        server.expect(once(), requestTo(BASE_URL + "/repos/taskmind/app/issues?state=open&per_page=1"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> github.importIssues(BASE_URL, "bad", "taskmind/app", 1))
                .isInstanceOf(ProviderClientException.class)
                .extracting("errorCode", "retrySafe")
                .containsExactly("PROVIDER_AUTH_FAILED", false);

        server.reset();
        server.expect(once(), requestTo(BASE_URL + "/repos/taskmind/app/issues?state=open&per_page=1"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> github.importIssues(BASE_URL, "token", "taskmind/app", 1))
                .isInstanceOf(ProviderClientException.class)
                .extracting("errorCode", "retrySafe")
                .containsExactly("PROVIDER_RATE_LIMITED", true);

        server.reset();
        server.expect(once(), requestTo(BASE_URL + "/repos/taskmind/app/issues?state=open&per_page=1"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThatThrownBy(() -> github.importIssues(BASE_URL, "token", "taskmind/app", 1))
                .isInstanceOf(ProviderClientException.class)
                .extracting("errorCode", "retrySafe")
                .containsExactly("PROVIDER_UNAVAILABLE", true);
    }
}
