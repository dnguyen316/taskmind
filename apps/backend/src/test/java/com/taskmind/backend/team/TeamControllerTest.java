package com.taskmind.backend.team;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static com.taskmind.backend.security.TestJwtSupport.jwtWithPermissions;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.taskmind.backend.analytics.application.AnalyticsRollupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(com.taskmind.backend.security.TestJwtSupport.Config.class)
class TeamControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnalyticsRollupRepository analytics;

    @Test
    void privilegedUserGetsSeededDirectory() throws Exception {
        when(analytics.assigneeWorkload()).thenReturn(List.of());
        mockMvc.perform(
                        get("/v1/team/directory")
                                .with(jwt("11111111-1111-1111-1111-111111111111", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembers").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members[?(@.email == 'superadmin@taskmind.local')]").exists());
    }

    @Test
    void nonPrivilegedUserForbidden() throws Exception {
        mockMvc.perform(get("/v1/team/directory").with(jwt("11111111-1111-1111-1111-111111111111")))
                .andExpect(status().isForbidden());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/v1/team/directory")).andExpect(status().isUnauthorized());
    }

    @Test
    void projectMembershipManagerCanAssignChangeAndRemoveViaTeamEndpoints() throws Exception {
        var ownerId = "11111111-1111-1111-1111-111111111111";
        var userId = "22222222-2222-2222-2222-222222222222";
        var projectId = createProject(ownerId);

        mockMvc.perform(post("/v1/team/members/{userId}/projects/{projectId}", userId, projectId)
                        .with(jwtWithPermissions(ownerId, List.of("project.members.manage")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"role": "MEMBER"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(patch("/v1/team/members/{userId}/projects/{projectId}/role", userId, projectId)
                        .with(jwtWithPermissions(ownerId, List.of("project.members.manage")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"role": "VIEWER"}
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VIEWER"));

        mockMvc.perform(delete("/v1/team/members/{userId}/projects/{projectId}", userId, projectId)
                        .with(jwtWithPermissions(ownerId, List.of("project.members.manage"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void projectMembershipAssignmentRequiresPermission() throws Exception {
        var ownerId = "11111111-1111-1111-1111-111111111111";
        var userId = "33333333-3333-3333-3333-333333333333";
        var projectId = createProject(ownerId);

        mockMvc.perform(post("/v1/team/members/{userId}/projects/{projectId}", userId, projectId)
                        .with(jwt(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"role": "MEMBER"}
                            """))
                .andExpect(status().isForbidden());
    }

    @Test
    void globalRoleAssignmentRequiresRbacPermission() throws Exception {
        mockMvc.perform(patch("/v1/team/members/{userId}/roles", "11111111-1111-1111-1111-111111111111")
                        .with(jwt("22222222-2222-2222-2222-222222222222"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"role": "MANAGER"}
                            """))
                .andExpect(status().isForbidden());
    }

    private String createProject(String ownerId) throws Exception {
        var projectPayload = """
            {
              "name": "Team management test project",
              "key": "%s",
              "ownerUserId": "%s"
            }
            """.formatted(java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase(), ownerId);

        var response = mockMvc.perform(post("/v1/projects")
                        .with(jwt(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectPayload))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(response.getResponse().getContentAsString()).get("id").asText();
    }

}
