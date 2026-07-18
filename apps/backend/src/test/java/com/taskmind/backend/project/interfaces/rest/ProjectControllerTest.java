package com.taskmind.backend.project.interfaces.rest;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(com.taskmind.backend.security.TestJwtSupport.Config.class)
class ProjectControllerTest {

    private static final String OWNER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String REQUESTED_OWNER_ID = "22222222-2222-2222-2222-222222222222";
    private static final String ADMIN_ID = "33333333-3333-3333-3333-333333333333";
    private static final String MEMBER_ID = "44444444-4444-4444-4444-444444444444";
    private static final String VIEWER_ID = "55555555-5555-5555-5555-555555555555";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsProjectForAuthenticatedUserWhenOwnerUserIdIsOmitted() throws Exception {
        mockMvc.perform(post("/v1/projects").with(jwt(OWNER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Personal project",
                      "key": "%s"
                    }
                    """.formatted(projectKey())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ownerUserId").value(OWNER_ID));
    }

    @Test
    void nonPrivilegedProjectCreationIgnoresRequestedOwnerUserId() throws Exception {
        mockMvc.perform(post("/v1/projects").with(jwt(OWNER_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Personal project",
                      "key": "%s",
                      "ownerUserId": "%s"
                    }
                    """.formatted(projectKey(), REQUESTED_OWNER_ID)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ownerUserId").value(OWNER_ID));
    }

    @Test
    void privilegedProjectCreationCanAssignOwnerUserId() throws Exception {
        mockMvc.perform(post("/v1/projects").with(jwt(OWNER_ID, "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Delegated project",
                      "key": "%s",
                      "ownerUserId": "%s"
                    }
                    """.formatted(projectKey(), REQUESTED_OWNER_ID)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ownerUserId").value(REQUESTED_OWNER_ID));
    }



    @Test
    void conflictProblemDetailsDoNotExposeInternalMessages() throws Exception {
        var key = projectKey();
        mockMvc.perform(post("/v1/projects").with(jwt(OWNER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "First", "key": "%s"}
                            """.formatted(key)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/projects").with(jwt(OWNER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Duplicate", "key": "%s"}
                            """.formatted(key)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PUBLIC_REQUEST_CONFLICT"))
                .andExpect(content().string(not(containsString("IllegalArgumentException"))))
                .andExpect(content().string(not(containsString("SQLException"))))
                .andExpect(content().string(not(containsString("select *"))))
                .andExpect(content().string(not(containsString(key))));
    }

    @Test
    void projectRoleCapabilityMatrixControlsReadUpdateAndArchive() throws Exception {
        assertProjectCapabilities(OWNER_ID, null, true, true, true);
        assertProjectCapabilities(ADMIN_ID, "ADMIN", true, true, true);
        assertProjectCapabilities(MEMBER_ID, "MEMBER", true, false, false);
        assertProjectCapabilities(VIEWER_ID, "VIEWER", true, false, false);
    }

    @Test
    void hidesProjectHealthFromNonMember() throws Exception {
        var projectKey = projectKey();
        var result = mockMvc.perform(post("/v1/projects").with(jwt(OWNER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Private project",
                              "key": "%s"
                            }
                            """.formatted(projectKey)))
                .andExpect(status().isCreated())
                .andReturn();

        var projectId = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();

        mockMvc.perform(get("/v1/projects/{id}/health", projectId).with(jwt(REQUESTED_OWNER_ID)))
                .andExpect(status().isNotFound());
    }


    private void assertProjectCapabilities(
            String actorId, String role, boolean canRead, boolean canUpdate, boolean canArchive) throws Exception {
        var updateProjectId = createProject();
        if (role != null) {
            addMemberAsOwner(updateProjectId, actorId, role);
        }

        mockMvc.perform(get("/v1/projects/{id}", updateProjectId).with(jwt(actorId)))
                .andExpect(canRead ? status().isOk() : status().isNotFound());

        mockMvc.perform(patch("/v1/projects/{id}", updateProjectId)
                        .with(jwt(actorId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"name": "Updated project", "key": "%s"}
                            """.formatted(projectKey())))
                .andExpect(canUpdate ? status().isOk() : status().isForbidden());

        var archiveProjectId = createProject();
        if (role != null) {
            addMemberAsOwner(archiveProjectId, actorId, role);
        }

        mockMvc.perform(patch("/v1/projects/{id}/archive", archiveProjectId).with(jwt(actorId)))
                .andExpect(canArchive ? status().isOk() : status().isForbidden());
    }

    private String createProject() throws Exception {
        var result = mockMvc.perform(post("/v1/projects").with(jwt(OWNER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "name": "Role matrix project",
                              "key": "%s"
                            }
                            """.formatted(projectKey())))
                .andExpect(status().isCreated())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id").toString();
    }

    private void addMemberAsOwner(String projectId, String userId, String role) throws Exception {
        mockMvc.perform(post("/v1/projects/{projectId}/members", projectId)
                        .with(jwt(OWNER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"userId": "%s", "role": "%s"}
                            """.formatted(userId, role)))
                .andExpect(status().isCreated());
    }

    private String projectKey() {
        return java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
