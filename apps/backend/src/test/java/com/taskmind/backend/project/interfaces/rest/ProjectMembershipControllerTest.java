package com.taskmind.backend.project.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProjectMembershipControllerTest {

    private static final String OWNER_ID = "11111111-1111-1111-1111-111111111111";
    private static final String ADMIN_ID = "33333333-3333-3333-3333-333333333333";
    private static final String MEMBER_ID = "22222222-2222-2222-2222-222222222222";
    private static final String NON_MEMBER_ID = "44444444-4444-4444-4444-444444444444";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void forbiddenAddAndRemoveByNonAdminMember() throws Exception {
        var projectId = createProject();
        addMemberAsOwner(projectId, MEMBER_ID, "MEMBER");

        mockMvc.perform(post("/v1/projects/{projectId}/members", projectId)
                .header("X-Actor-User-Id", MEMBER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": "55555555-5555-5555-5555-555555555555", "role": "MEMBER"}
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(delete("/v1/projects/{projectId}/members/{userId}", projectId, MEMBER_ID)
                .header("X-Actor-User-Id", MEMBER_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    void allowedAddAndRemoveByOwnerAndAdmin() throws Exception {
        var projectId = createProject();

        addMemberAsOwner(projectId, ADMIN_ID, "ADMIN");

        mockMvc.perform(post("/v1/projects/{projectId}/members", projectId)
                .header("X-Actor-User-Id", ADMIN_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": "66666666-6666-6666-6666-666666666666", "role": "MEMBER"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/v1/projects/{projectId}/members/{userId}", projectId, "66666666-6666-6666-6666-666666666666")
                .header("X-Actor-User-Id", OWNER_ID))
            .andExpect(status().isNoContent());
    }

    @Test
    void forbiddenListingByNonMember() throws Exception {
        var projectId = createProject();
        addMemberAsOwner(projectId, MEMBER_ID, "MEMBER");

        mockMvc.perform(get("/v1/projects/{projectId}/members", projectId)
                .header("X-Actor-User-Id", NON_MEMBER_ID))
            .andExpect(status().isForbidden());
    }

    private String createProject() throws Exception {
        var projectPayload = """
            {
              "name": "Membership test project",
              "key": "MTP",
              "ownerUserId": "%s"
            }
            """.formatted(OWNER_ID);

        var createProjectResponse = mockMvc.perform(post("/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(projectPayload))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readTree(createProjectResponse.getResponse().getContentAsString()).get("id").asText();
    }

    private void addMemberAsOwner(String projectId, String userId, String role) throws Exception {
        var addMemberPayload = """
            {
              "userId": "%s",
              "role": "%s"
            }
            """.formatted(userId, role);

        mockMvc.perform(post("/v1/projects/{projectId}/members", projectId)
                .header("X-Actor-User-Id", OWNER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addMemberPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.projectId").value(projectId))
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.role").value(role));
    }
}
