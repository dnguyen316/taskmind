package com.taskmind.backend.team;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static com.taskmind.backend.security.TestJwtSupport.jwtWithPermissions;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.taskmind.backend.analytics.application.AnalyticsRollupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.AuthJpaEnums;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserJpaEntity;
import com.taskmind.backend.auth.infrastructure.persistence.jpa.UserJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
    @Autowired UserJpaRepository users;

    @Test
    void adminSeesActiveUsers() throws Exception {
        var activeUser = createUser("60000000-0000-0000-0000-000000000001", AuthJpaEnums.UserStatus.ACTIVE);
        when(analytics.assigneeWorkload()).thenReturn(List.of());

        mockMvc.perform(get("/v1/team/directory")
                        .with(jwt("11111111-1111-1111-1111-111111111111", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembers").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members[?(@.email == '" + activeUser.getPrimaryEmail() + "')]").exists());
    }

    @Test
    void managerSeesOnlyUsersInManagedProjects() throws Exception {
        var managerId = "60000000-0000-0000-0000-000000000010";
        var visibleUser = createUser("60000000-0000-0000-0000-000000000011", AuthJpaEnums.UserStatus.ACTIVE);
        var hiddenUser = createUser("60000000-0000-0000-0000-000000000012", AuthJpaEnums.UserStatus.ACTIVE);
        var managedProjectId = createProject(managerId);
        var unmanagedProjectId = createProject("60000000-0000-0000-0000-000000000099");
        assignProjectMember(managerId, visibleUser.getId().toString(), managedProjectId);
        assignProjectMember("60000000-0000-0000-0000-000000000099", hiddenUser.getId().toString(), unmanagedProjectId);
        when(analytics.assigneeWorkload()).thenReturn(List.of());

        mockMvc.perform(get("/v1/team/directory").with(jwt(managerId, "MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[?(@.email == '" + visibleUser.getPrimaryEmail() + "')]").exists())
                .andExpect(jsonPath("$.members[?(@.email == '" + hiddenUser.getPrimaryEmail() + "')]").doesNotExist());
    }

    @Test
    void memberCannotEnumerateWholeDirectory() throws Exception {
        mockMvc.perform(get("/v1/team/directory")
                        .with(jwt("11111111-1111-1111-1111-111111111111", "MEMBER")))
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("SecurityException"))))
                .andExpect(content().string(not(containsString("java.lang"))))
                .andExpect(content().string(not(containsString("token="))))
                .andExpect(content().string(not(containsString("select *"))));
    }

    @Test
    void inactiveUsersAreExcludedByDefault() throws Exception {
        var inactiveUser = createUser("60000000-0000-0000-0000-000000000020", AuthJpaEnums.UserStatus.DISABLED);
        when(analytics.assigneeWorkload()).thenReturn(List.of());

        mockMvc.perform(get("/v1/team/directory")
                        .with(jwt("11111111-1111-1111-1111-111111111111", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members[?(@.email == '" + inactiveUser.getPrimaryEmail() + "')]").doesNotExist());
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
                .andExpect(status().isForbidden())
                .andExpect(content().string(not(containsString("ProjectMembershipForbiddenException"))))
                .andExpect(content().string(not(containsString("java.lang"))))
                .andExpect(content().string(not(containsString("token="))))
                .andExpect(content().string(not(containsString("select *"))));
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

    private UserJpaEntity createUser(String id, AuthJpaEnums.UserStatus status) {
        var uuid = UUID.fromString(id);
        return users.findById(uuid)
                .orElseGet(() -> users.save(new UserJpaEntity(
                        uuid,
                        status,
                        "team-" + uuid + "@taskmind.local",
                        "hash",
                        "Team " + uuid,
                        Instant.now())));
    }

    private void assignProjectMember(String ownerId, String userId, String projectId) throws Exception {
        mockMvc.perform(post("/v1/team/members/{userId}/projects/{projectId}", userId, projectId)
                        .with(jwtWithPermissions(ownerId, List.of("project.members.manage")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"role": "MEMBER"}
                            """))
                .andExpect(status().isCreated());
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
