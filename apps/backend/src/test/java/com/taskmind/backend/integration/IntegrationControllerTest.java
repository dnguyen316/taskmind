package com.taskmind.backend.integration;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.integration.infrastructure.github.GitHubClient;
import com.taskmind.backend.integration.infrastructure.jira.JiraCloudClient;
import com.taskmind.backend.integration.infrastructure.wiki.WikiClient;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(com.taskmind.backend.security.TestJwtSupport.Config.class)
class IntegrationControllerTest {
    private static final AtomicReference<List<JiraCloudClient.ExternalIssue>> JIRA_ISSUES = new AtomicReference<>(defaultJiraIssues("TM"));
    private static final ConcurrentHashMap<String, AtomicInteger> JIRA_PUBLISH_COUNTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, AtomicInteger> WIKI_PUBLISH_COUNTS = new ConcurrentHashMap<>();
    private static final AtomicInteger GITHUB_COMMENT_COUNT = new AtomicInteger();
    private static volatile boolean GITHUB_FAIL_ISSUE = false;
    private static volatile boolean GITHUB_FAIL_REPOSITORY = false;

    @TestConfiguration
    static class ProviderClientStubs {
        @Bean
        @Primary
        JiraCloudClient jiraCloudClient() {
            return new JiraCloudClient(org.springframework.web.client.RestClient.builder()) {
                @Override
                public java.util.List<ExternalIssue> importIssues(String baseUrl, String accessToken, String externalProjectKey, int limit) {
                    return JIRA_ISSUES.get().stream().limit(limit).toList();
                }

                @Override
                public PublishedIssue publish(String baseUrl, String accessToken, String projectKey, String title, String type) {
                    int count = JIRA_PUBLISH_COUNTS.computeIfAbsent(projectKey, ignored -> new AtomicInteger()).incrementAndGet();
                    String key = projectKey + "-PUB-" + count;
                    return new PublishedIssue(projectKey + "-id-" + count, key, "https://example.test/browse/" + key);
                }
            };
        }

        @Bean
        @Primary
        GitHubClient gitHubClient() {
            return new GitHubClient(org.springframework.web.client.RestClient.builder()) {
                @Override
                public RepositoryMetadata getRepository(String baseUrl, String accessToken, String owner, String repo) {
                    if (GITHUB_FAIL_REPOSITORY) throw new com.taskmind.backend.integration.infrastructure.ProviderClientException(org.springframework.http.HttpStatus.BAD_GATEWAY, "PROVIDER_UNAVAILABLE", "GitHub request failed for https://api.github.test/repos/" + owner + "/" + repo + " token=ghp_secret", true);
                    return new RepositoryMetadata("repo-node-1", owner, repo, owner + "/" + repo, "main", false, "https://github.com/" + owner + "/" + repo, "99", "42");
                }

                @Override
                public Issue getIssue(String baseUrl, String accessToken, String owner, String repo, int issueNumber) {
                    if (GITHUB_FAIL_ISSUE) throw new com.taskmind.backend.integration.infrastructure.ProviderClientException(org.springframework.http.HttpStatus.BAD_GATEWAY, "PROVIDER_UNAVAILABLE", "GitHub request failed", true);
                    return new Issue("issue-node-" + issueNumber, issueNumber, "Issue " + issueNumber, "Body", "open", "https://github.test/issue/" + issueNumber);
                }

                @Override
                public Comment createIssueComment(String baseUrl, String accessToken, String owner, String repo, int issueNumber, String body) {
                    int count = GITHUB_COMMENT_COUNT.incrementAndGet();
                    return new Comment("comment-node-" + count, body, "nova", "https://github.test/comment/" + count);
                }

                @Override
                public PullRequestStatus getPullRequestStatus(String baseUrl, String accessToken, String owner, String repo, int pullNumber) {
                    return new PullRequestStatus("pr-node-" + pullNumber, pullNumber, "open", true, false, "abc123");
                }

                @Override
                public Ref createBranchRef(String baseUrl, String accessToken, String owner, String repo, String branchName, String fromSha) {
                    return new Ref("refs/heads/" + branchName, fromSha);
                }

                @Override
                public PullRequest createPullRequest(String baseUrl, String accessToken, String owner, String repo, String title, String head, String base, String body) {
                    return new PullRequest("pr-node-new", 42, title, "open", "https://github.test/pr/42", "def456");
                }
            };
        }

        @Bean
        @Primary
        WikiClient wikiClient() {
            return new WikiClient(org.springframework.web.client.RestClient.builder()) {
                @Override
                public PublishedPage publish(String baseUrl, String accessToken, String spaceKey, String title) {
                    int count = WIKI_PUBLISH_COUNTS.computeIfAbsent(spaceKey, ignored -> new AtomicInteger()).incrementAndGet();
                    String key = spaceKey + ":" + count;
                    return new PublishedPage(spaceKey + "-page-" + count, key, "https://example.test/wiki/" + spaceKey + "/" + count);
                }
            };
        }
    }
    private static final String USER = "11111111-1111-1111-1111-111111111111";
    private static final String OTHER = "22222222-2222-2222-2222-222222222222";
    @Autowired MockMvc mockMvc; @Autowired ObjectMapper mapper;

    @Test void connectListLinkImportAndPublishWithoutReturningSecrets() throws Exception {
        JIRA_ISSUES.set(defaultJiraIssues("TM-FLOW"));
        String projectId = createProject(USER);
        String connectionId = connect("JIRA", USER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.provider").value("JIRA"))
                .andReturn().getResponse().getContentAsString();
        connectionId = mapper.readTree(connectionId).get("id").asText();
        mockMvc.perform(get("/v1/integrations/connections").with(jwt(USER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].accessToken").doesNotExist());
        String linkId = mockMvc.perform(post("/v1/integrations/projects/{projectId}/links", projectId).with(jwt(USER)).contentType(MediaType.APPLICATION_JSON).content("""
                {"connectionId":"%s","externalProjectId":"10001","externalProjectKey":"TM","externalProjectName":"TaskMind","metadataJson":"{\\\"boardId\\\":7}"}
                """.formatted(connectionId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.externalProjectKey").value("TM"))
                .andReturn().getResponse().getContentAsString();
        linkId = mapper.readTree(linkId).get("id").asText();
        mockMvc.perform(post("/v1/integrations/project-links/{id}/imports", linkId).with(jwt(USER)).contentType(MediaType.APPLICATION_JSON).content("{\"limit\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED")).andExpect(jsonPath("$.importedCount").value(2)).andExpect(jsonPath("$.skippedCount").value(0));
        String taskId = createTask(USER, projectId);
        mockMvc.perform(post("/v1/tasks/{taskId}/integrations/jira/publish", taskId).with(jwt(USER)).contentType(MediaType.APPLICATION_JSON).content("{\"projectLinkId\":\"" + linkId + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLISHED")).andExpect(jsonPath("$.externalKey", startsWith("TM-")));
    }


    @Test void publishesSameTaskToTwoJiraLinksAndRepublishIsIdempotentPerLink() throws Exception {
        String projectId = createProject(USER);
        String connectionId = mapper.readTree(connect("JIRA", USER).andReturn().getResponse().getContentAsString()).get("id").asText();
        String firstLinkId = createProjectLink(USER, projectId, connectionId, "JIRA-A-project", "JIRA-A");
        String secondLinkId = createProjectLink(USER, projectId, connectionId, "JIRA-B-project", "JIRA-B");
        String taskId = createTask(USER, projectId);

        String firstRecord = publish(taskId, firstLinkId, "jira")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectLinkId").value(firstLinkId))
                .andExpect(jsonPath("$.externalKey").value("JIRA-A-PUB-1"))
                .andReturn().getResponse().getContentAsString();
        String firstRecordId = mapper.readTree(firstRecord).get("id").asText();

        publish(taskId, secondLinkId, "jira")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectLinkId").value(secondLinkId))
                .andExpect(jsonPath("$.externalKey").value("JIRA-B-PUB-1"));

        publish(taskId, firstLinkId, "jira")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstRecordId))
                .andExpect(jsonPath("$.externalKey").value("JIRA-A-PUB-1"));
        org.assertj.core.api.Assertions.assertThat(JIRA_PUBLISH_COUNTS.get("JIRA-A").get()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(JIRA_PUBLISH_COUNTS.get("JIRA-B").get()).isEqualTo(1);
    }

    @Test void publishesSameTaskToTwoWikiSpacesAndRepublishIsIdempotentPerLink() throws Exception {
        String projectId = createProject(USER);
        String connectionId = mapper.readTree(connect("WIKI", USER).andReturn().getResponse().getContentAsString()).get("id").asText();
        String firstLinkId = createProjectLink(USER, projectId, connectionId, "SPACE-A-id", "SPACEA");
        String secondLinkId = createProjectLink(USER, projectId, connectionId, "SPACE-B-id", "SPACEB");
        String taskId = createTask(USER, projectId);

        String firstRecord = publish(taskId, firstLinkId, "wiki")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectLinkId").value(firstLinkId))
                .andExpect(jsonPath("$.externalKey").value("SPACEA:1"))
                .andReturn().getResponse().getContentAsString();
        String firstRecordId = mapper.readTree(firstRecord).get("id").asText();

        publish(taskId, secondLinkId, "wiki")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectLinkId").value(secondLinkId))
                .andExpect(jsonPath("$.externalKey").value("SPACEB:1"));

        publish(taskId, firstLinkId, "wiki")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstRecordId))
                .andExpect(jsonPath("$.externalKey").value("SPACEA:1"));
        org.assertj.core.api.Assertions.assertThat(WIKI_PUBLISH_COUNTS.get("SPACEA").get()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(WIKI_PUBLISH_COUNTS.get("SPACEB").get()).isEqualTo(1);
    }

    @Test void discoversAndLinksGitHubRepositoryWithAllowedOperations() throws Exception {
        String projectId = createProject(USER);
        String connectionId = mapper.readTree(connect("GITHUB", USER).andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/v1/integrations/github/repositories/{owner}/{repo}", "taskmind", "core").queryParam("connectionId", connectionId).with(jwt(USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("taskmind/core"))
                .andExpect(jsonPath("$.defaultBranch").value("main"));

        mockMvc.perform(post("/v1/integrations/github/projects/{projectId}/repositories", projectId).with(jwt(USER)).contentType(MediaType.APPLICATION_JSON).content("""
                {"connectionId":"%s","owner":"taskmind","repo":"core","allowedOperations":["READ_ISSUES","CREATE_PR","COMMENT"]}
                """.formatted(connectionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("GITHUB"))
                .andExpect(jsonPath("$.externalProjectId").value("taskmind/core"))
                .andExpect(jsonPath("$.repositoryOwner").value("taskmind"))
                .andExpect(jsonPath("$.repositoryName").value("core"))
                .andExpect(jsonPath("$.defaultBranch").value("main"))
                .andExpect(jsonPath("$.allowedOperationsJson", containsString("CREATE_PR")));
    }



    @Test void internalGitHubEndpointsRequireServiceTokenAndPermissionFlags() throws Exception {
        GITHUB_FAIL_ISSUE = false;
        String projectId = createProject(USER);
        String connectionId = mapper.readTree(connect("GITHUB", USER).andReturn().getResponse().getContentAsString()).get("id").asText();
        String linkId = createGitHubRepositoryLink(USER, projectId, connectionId, "READ_ISSUES");

        internalGetIssue(linkId, projectId, USER, null).andExpect(status().isUnauthorized());
        internalGetIssue(linkId, projectId, USER, "test-only-nova-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value(7))
                .andExpect(jsonPath("$.id").value("issue-node-7"));
        mockMvc.perform(post("/internal/integrations/github/repos/{linkId}/comments", linkId)
                        .header("X-Service-Token", "test-only-nova-token")
                        .header("X-TaskMind-User-Id", USER)
                        .header("X-TaskMind-Project-Id", projectId)
                        .header("Idempotency-Key", "deny-comment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"issueNumber\":7,\"body\":\"Nope\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail", containsString("COMMENT")));
    }

    @Test void internalGitHubEndpointRejectsUnknownRepoLinkAndOutOfScopeProject() throws Exception {
        String projectId = createProject(USER);
        internalGetIssue(java.util.UUID.randomUUID().toString(), projectId, USER, "test-only-nova-token")
                .andExpect(status().isNotFound());
        String otherProject = createProject(USER);
        String connectionId = mapper.readTree(connect("GITHUB", USER).andReturn().getResponse().getContentAsString()).get("id").asText();
        String linkId = createGitHubRepositoryLink(USER, projectId, connectionId, "READ_ISSUES");
        internalGetIssue(linkId, otherProject, USER, "test-only-nova-token")
                .andExpect(status().isForbidden());
    }

    @Test void internalGitHubProviderFailureMapsWithoutSecrets() throws Exception {
        String projectId = createProject(USER);
        String connectionId = mapper.readTree(connect("GITHUB", USER).andReturn().getResponse().getContentAsString()).get("id").asText();
        String linkId = createGitHubRepositoryLink(USER, projectId, connectionId, "READ_ISSUES");
        GITHUB_FAIL_ISSUE = true;
        internalGetIssue(linkId, projectId, USER, "test-only-nova-token")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("PROVIDER_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail", not(containsString("secret-access"))));
        GITHUB_FAIL_ISSUE = false;
    }

    @Test void internalGitHubMutationsAreIdempotentByKey() throws Exception {
        GITHUB_COMMENT_COUNT.set(0);
        String projectId = createProject(USER);
        String connectionId = mapper.readTree(connect("GITHUB", USER).andReturn().getResponse().getContentAsString()).get("id").asText();
        String linkId = createGitHubRepositoryLink(USER, projectId, connectionId, "COMMENT");
        String body = "{\"issueNumber\":7,\"body\":\"Done\"}";
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/internal/integrations/github/repos/{linkId}/comments", linkId)
                            .header("X-Service-Token", "test-only-nova-token")
                            .header("X-TaskMind-User-Id", USER)
                            .header("X-TaskMind-Project-Id", projectId)
                            .header("Idempotency-Key", "same-key")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("comment-node-1"));
        }
        org.assertj.core.api.Assertions.assertThat(GITHUB_COMMENT_COUNT.get()).isEqualTo(1);
    }

    @Test void publicGitHubProviderFailureReturnsSanitizedProblemDetails() throws Exception {
        String connectionId = mapper.readTree(connect("GITHUB", USER).andReturn().getResponse().getContentAsString())
                .get("id")
                .asText();
        GITHUB_FAIL_REPOSITORY = true;
        try {
            mockMvc.perform(get("/v1/integrations/github/repositories/{owner}/{repo}", "taskmind", "core")
                            .param("connectionId", connectionId)
                            .with(jwt(USER)))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.detail").value("Integration provider request failed."))
                    .andExpect(content().string(not(containsString("https://api.github.test"))))
                    .andExpect(content().string(not(containsString("ghp_secret"))))
                    .andExpect(content().string(not(containsString("ProviderClientException"))))
                    .andExpect(content().string(not(containsString("java.lang"))));
        } finally {
            GITHUB_FAIL_REPOSITORY = false;
        }
    }

    @Test void repeatedJiraImportSkipsPreviouslyImportedIssues() throws Exception {
        String prefix = "TM-REPEAT-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        JIRA_ISSUES.set(defaultJiraIssues(prefix));
        String linkId = createJiraProjectLink(USER, prefix);

        mockMvc.perform(post("/v1/integrations/project-links/{id}/imports", linkId).with(jwt(USER)).contentType(MediaType.APPLICATION_JSON).content("{\"limit\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(0));
        mockMvc.perform(post("/v1/integrations/project-links/{id}/imports", linkId).with(jwt(USER)).contentType(MediaType.APPLICATION_JSON).content("{\"limit\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(0))
                .andExpect(jsonPath("$.skippedCount").value(2));
    }

    @Test void partialDuplicateJiraImportReportsImportedAndSkippedCounts() throws Exception {
        String prefix = "TM-PARTIAL-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        JIRA_ISSUES.set(defaultJiraIssues(prefix));
        String linkId = createJiraProjectLink(USER, prefix);

        mockMvc.perform(post("/v1/integrations/project-links/{id}/imports", linkId).with(jwt(USER)).contentType(MediaType.APPLICATION_JSON).content("{\"limit\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(0));

        JIRA_ISSUES.set(List.of(
                new JiraCloudClient.ExternalIssue(prefix + "-id-2", prefix + "-2", "Imported 2 again", "Body 2"),
                new JiraCloudClient.ExternalIssue(prefix + "-id-3", prefix + "-3", "Imported 3", "Body 3")));
        mockMvc.perform(post("/v1/integrations/project-links/{id}/imports", linkId).with(jwt(USER)).contentType(MediaType.APPLICATION_JSON).content("{\"limit\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(1))
                .andExpect(jsonPath("$.skippedCount").value(1));
    }

    @Test void rejectsUnauthorizedProjectScopeAndValidationFailures() throws Exception {
        String projectId = createProject(USER); String connectionId = mapper.readTree(connect("GITHUB", USER).andReturn().getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/v1/integrations/projects/{projectId}/links", projectId).with(jwt(OTHER)).contentType(MediaType.APPLICATION_JSON).content("{\"connectionId\":\"" + connectionId + "\",\"externalProjectId\":\"org/repo\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/integrations/GITHUB/connections").with(jwt(USER)).contentType(MediaType.APPLICATION_JSON).content("{\"accountName\":\"bad\"}"))
                .andExpect(status().isBadRequest());
    }

    private String createGitHubRepositoryLink(String user, String projectId, String connectionId, String... operations) throws Exception { org.springframework.test.web.servlet.MvcResult res = mockMvc.perform(post("/v1/integrations/github/projects/{projectId}/repositories", projectId).with(jwt(user)).contentType(MediaType.APPLICATION_JSON).content("{\"connectionId\":\"" + connectionId + "\",\"owner\":\"taskmind\",\"repo\":\"core\",\"allowedOperations\":[" + java.util.Arrays.stream(operations).map(op -> "\"" + op + "\"").collect(java.util.stream.Collectors.joining(",")) + "]}" )).andExpect(status().isCreated()).andReturn(); return mapper.readTree(res.getResponse().getContentAsString()).get("id").asText(); }
    private org.springframework.test.web.servlet.ResultActions internalGetIssue(String linkId, String projectId, String userId, String token) throws Exception { var builder = get("/internal/integrations/github/repos/{linkId}/issues/{issueNumber}", linkId, 7).header("X-TaskMind-User-Id", userId).header("X-TaskMind-Project-Id", projectId); if (token != null) builder.header("X-Service-Token", token); return mockMvc.perform(builder); }
    private String createJiraProjectLink(String user, String key) throws Exception { String projectId = createProject(user); String connectionId = mapper.readTree(connect("JIRA", user).andReturn().getResponse().getContentAsString()).get("id").asText(); return createProjectLink(user, projectId, connectionId, key + "-project", key); }
    private String createProjectLink(String user, String projectId, String connectionId, String externalProjectId, String externalProjectKey) throws Exception { org.springframework.test.web.servlet.MvcResult res = mockMvc.perform(post("/v1/integrations/projects/{projectId}/links", projectId).with(jwt(user)).contentType(MediaType.APPLICATION_JSON).content("{\"connectionId\":\"" + connectionId + "\",\"externalProjectId\":\"" + externalProjectId + "\",\"externalProjectKey\":\"" + externalProjectKey + "\",\"externalProjectName\":\"TaskMind\"}" )).andExpect(status().isCreated()).andReturn(); return mapper.readTree(res.getResponse().getContentAsString()).get("id").asText(); }
    private org.springframework.test.web.servlet.ResultActions publish(String taskId, String linkId, String providerPath) throws Exception { return mockMvc.perform(post("/v1/tasks/{taskId}/integrations/" + providerPath + "/publish", taskId).with(jwt(USER)).contentType(MediaType.APPLICATION_JSON).content("{\"projectLinkId\":\"" + linkId + "\"}")); }
    private static List<JiraCloudClient.ExternalIssue> defaultJiraIssues(String prefix) { return List.of(new JiraCloudClient.ExternalIssue(prefix + "-id-1", prefix + "-1", "Imported 1", "Body 1"), new JiraCloudClient.ExternalIssue(prefix + "-id-2", prefix + "-2", "Imported 2", "Body 2")); }

    private org.springframework.test.web.servlet.ResultActions connect(String provider, String user) throws Exception { return mockMvc.perform(post("/v1/integrations/{provider}/connections", provider).with(jwt(user)).contentType(MediaType.APPLICATION_JSON).content("""
            {"accountName":"acct","baseUrl":"https://example.test","accountExternalId":"acct-1","accessToken":"secret-access","refreshToken":"secret-refresh","scopes":"read write"}
            """)); }
    private String createProject(String user) throws Exception { org.springframework.test.web.servlet.MvcResult res = mockMvc.perform(post("/v1/projects").with(jwt(user)).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Integrations\",\"key\":\"" + java.util.UUID.randomUUID().toString().substring(0,8).toUpperCase() + "\"}" )).andExpect(status().isCreated()).andReturn(); return mapper.readTree(res.getResponse().getContentAsString()).get("id").asText(); }
    private String createTask(String user, String projectId) throws Exception { org.springframework.test.web.servlet.MvcResult res = mockMvc.perform(post("/v1/tasks").with(jwt(user)).contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"" + user + "\",\"projectId\":\"" + projectId + "\",\"title\":\"Publish me\",\"status\":\"TODO\",\"priority\":3,\"source\":\"MANUAL\"}" )).andExpect(status().isCreated()).andReturn(); return mapper.readTree(res.getResponse().getContentAsString()).get("id").asText(); }
}
