package com.taskmind.backend.tasktype;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.backend.project.domain.model.ProjectMembership;
import com.taskmind.backend.project.domain.model.ProjectMembershipRole;
import com.taskmind.backend.project.domain.repository.ProjectMembershipRepository;
import com.taskmind.backend.project.domain.repository.ProjectRepository;
import com.taskmind.backend.security.AuthenticatedUserResolver;
import com.taskmind.backend.security.JwtClaimAuthenticationConverter;
import com.taskmind.backend.security.SecurityConfig;
import com.taskmind.backend.security.TestJwtSupport;
import com.taskmind.backend.task.domain.model.TaskLevel;
import com.taskmind.backend.tasktype.application.TaskTypeApplicationService;
import com.taskmind.backend.tasktype.domain.model.TaskTypeDefinition;
import com.taskmind.backend.tasktype.domain.repository.TaskTypeRepository;
import com.taskmind.backend.tasktype.interfaces.rest.TaskTypeController;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TaskTypeController.class)
@Import({
    SecurityConfig.class,
    AuthenticatedUserResolver.class,
    JwtClaimAuthenticationConverter.class,
    TestJwtSupport.Config.class,
    TaskTypeApplicationService.class
})
class TaskTypeControllerTest {
    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MEMBER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID NON_MEMBER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ADMIN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PROJECT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID TASK_TYPE_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired private MockMvc mockMvc;

    @MockBean private TaskTypeRepository taskTypes;
    @MockBean private ProjectRepository projects;
    @MockBean private ProjectMembershipRepository memberships;

    @Test
    void nonMemberCannotListAnotherProjectsCustomTypes() throws Exception {
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(memberships.existsByProjectIdAndUserId(PROJECT_ID, NON_MEMBER_ID)).thenReturn(false);
        when(taskTypes.findActive(null)).thenReturn(List.of(systemType()));

        mockMvc.perform(get("/v1/task-types").param("projectId", PROJECT_ID.toString()).with(jwt(NON_MEMBER_ID.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].key").value("TASK"));
    }

    @Test
    void nonMemberCannotCreateProjectTaskType() throws Exception {
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(memberships.findByProjectIdAndUserId(PROJECT_ID, NON_MEMBER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/v1/task-types")
                        .with(jwt(NON_MEMBER_ID.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isForbidden());

        verify(taskTypes, never()).save(any());
    }

    @Test
    void nonMemberCannotPatchProjectTaskType() throws Exception {
        when(taskTypes.findById(TASK_TYPE_ID)).thenReturn(Optional.of(projectType()));
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(memberships.findByProjectIdAndUserId(PROJECT_ID, NON_MEMBER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/v1/task-types/{id}", TASK_TYPE_ID)
                        .with(jwt(NON_MEMBER_ID.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Bug\"}"))
                .andExpect(status().isForbidden());

        verify(taskTypes, never()).save(any());
    }

    @Test
    void ownerMemberAndAdminCanListProjectTaskTypes() throws Exception {
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(memberships.existsByProjectIdAndUserId(PROJECT_ID, MEMBER_ID)).thenReturn(true);
        when(taskTypes.findActive(PROJECT_ID)).thenReturn(List.of(systemType(), projectType()));

        mockMvc.perform(get("/v1/task-types").param("projectId", PROJECT_ID.toString()).with(jwt(OWNER_ID.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/v1/task-types").param("projectId", PROJECT_ID.toString()).with(jwt(MEMBER_ID.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(get("/v1/task-types").param("projectId", PROJECT_ID.toString()).with(jwt(ADMIN_ID.toString(), "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void ownerMemberAdminManagementPermissionsAreEnforced() throws Exception {
        when(projects.findById(PROJECT_ID)).thenReturn(Optional.of(project()));
        when(memberships.findByProjectIdAndUserId(PROJECT_ID, MEMBER_ID))
                .thenReturn(Optional.of(new ProjectMembership(PROJECT_ID, MEMBER_ID, ProjectMembershipRole.ADMIN)));
        when(taskTypes.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskTypes.findById(TASK_TYPE_ID)).thenReturn(Optional.of(projectType()));

        mockMvc.perform(post("/v1/task-types")
                        .with(jwt(OWNER_ID.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(PROJECT_ID.toString()));
        mockMvc.perform(patch("/v1/task-types/{id}", TASK_TYPE_ID)
                        .with(jwt(MEMBER_ID.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated Bug\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Bug"));
        mockMvc.perform(post("/v1/task-types")
                        .with(jwt(ADMIN_ID.toString(), "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isCreated());
    }

    private static Project project() {
        return new Project(PROJECT_ID, null, "Project", "PROJ", null, OWNER_ID, null, NOW, NOW);
    }

    private static TaskTypeDefinition systemType() {
        return TaskTypeDefinition.system(UUID.randomUUID(), "TASK", "Task", TaskLevel.TASK, Set.of(TaskLevel.TASK), false, false, TaskTypeDefinition.SystemKind.TASK, 1, NOW);
    }

    private static TaskTypeDefinition projectType() {
        return new TaskTypeDefinition(TASK_TYPE_ID, null, PROJECT_ID, "BUG", "Bug", "#f00", "bug", TaskLevel.TASK, Set.of(TaskLevel.TASK), false, false, null, false, true, 2, NOW, NOW);
    }

    private static String createBody() {
        return """
                {
                  "projectId": "%s",
                  "key": "risk",
                  "name": "Risk",
                  "color": "#f59e0b",
                  "icon": "warning",
                  "sortOrder": 3
                }
                """.formatted(PROJECT_ID);
    }
}
