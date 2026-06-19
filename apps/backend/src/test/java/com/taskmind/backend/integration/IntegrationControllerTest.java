package com.taskmind.backend.integration;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.integration.infrastructure.jira.JiraCloudClient;
import java.util.List;
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
                    return new PublishedIssue("10003", "TM-3", "https://example.test/browse/TM-3");
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

    private String createJiraProjectLink(String user, String key) throws Exception { String projectId = createProject(user); String connectionId = mapper.readTree(connect("JIRA", user).andReturn().getResponse().getContentAsString()).get("id").asText(); org.springframework.test.web.servlet.MvcResult res = mockMvc.perform(post("/v1/integrations/projects/{projectId}/links", projectId).with(jwt(user)).contentType(MediaType.APPLICATION_JSON).content("{\"connectionId\":\"" + connectionId + "\",\"externalProjectId\":\"" + key + "-project\",\"externalProjectKey\":\"" + key + "\",\"externalProjectName\":\"TaskMind\"}" )).andExpect(status().isCreated()).andReturn(); return mapper.readTree(res.getResponse().getContentAsString()).get("id").asText(); }
    private static List<JiraCloudClient.ExternalIssue> defaultJiraIssues(String prefix) { return List.of(new JiraCloudClient.ExternalIssue(prefix + "-id-1", prefix + "-1", "Imported 1", "Body 1"), new JiraCloudClient.ExternalIssue(prefix + "-id-2", prefix + "-2", "Imported 2", "Body 2")); }

    private org.springframework.test.web.servlet.ResultActions connect(String provider, String user) throws Exception { return mockMvc.perform(post("/v1/integrations/{provider}/connections", provider).with(jwt(user)).contentType(MediaType.APPLICATION_JSON).content("""
            {"accountName":"acct","baseUrl":"https://example.test","accountExternalId":"acct-1","accessToken":"secret-access","refreshToken":"secret-refresh","scopes":"read write"}
            """)); }
    private String createProject(String user) throws Exception { org.springframework.test.web.servlet.MvcResult res = mockMvc.perform(post("/v1/projects").with(jwt(user)).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Integrations\",\"key\":\"" + java.util.UUID.randomUUID().toString().substring(0,8).toUpperCase() + "\"}" )).andExpect(status().isCreated()).andReturn(); return mapper.readTree(res.getResponse().getContentAsString()).get("id").asText(); }
    private String createTask(String user, String projectId) throws Exception { org.springframework.test.web.servlet.MvcResult res = mockMvc.perform(post("/v1/tasks").with(jwt(user)).contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"" + user + "\",\"projectId\":\"" + projectId + "\",\"title\":\"Publish me\",\"status\":\"TODO\",\"priority\":3,\"source\":\"MANUAL\"}" )).andExpect(status().isCreated()).andReturn(); return mapper.readTree(res.getResponse().getContentAsString()).get("id").asText(); }
}
