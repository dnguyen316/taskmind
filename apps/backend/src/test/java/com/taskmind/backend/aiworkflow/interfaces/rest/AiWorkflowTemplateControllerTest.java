package com.taskmind.backend.aiworkflow.interfaces.rest;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.taskmind.backend.aiworkflow.application.AiWorkflowTemplateApplicationService;
import com.taskmind.backend.aiworkflow.domain.model.AiWorkflowTemplate;
import com.taskmind.backend.aiworkflow.domain.repository.AiWorkflowTemplateRepository;
import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.backend.project.domain.model.ProjectMembership;
import com.taskmind.backend.project.domain.model.ProjectMembershipRole;
import com.taskmind.backend.project.domain.repository.ProjectMembershipRepository;
import com.taskmind.backend.project.domain.repository.ProjectRepository;
import com.taskmind.backend.security.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@WebMvcTest(controllers = AiWorkflowTemplateController.class)
@Import({SecurityConfig.class, AuthenticatedUserResolver.class, JwtClaimAuthenticationConverter.class,
        TestJwtSupport.Config.class, AiWorkflowTemplateControllerTest.Fakes.class})
class AiWorkflowTemplateControllerTest {
    static final String OWNER = "11111111-1111-1111-1111-111111111111";
    static final String MEMBER = "22222222-2222-2222-2222-222222222222";
    static final String OUTSIDER = "99999999-9999-9999-9999-999999999999";
    static final UUID PROJECT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired MockMvc mockMvc;
    @Autowired InMemoryAiWorkflowTemplateRepository templates;

    @BeforeEach
    void reset() {
        templates.reset();
    }

    @Test
    void ownerCanCreateListGetUpdateAndArchiveTemplate() throws Exception {
        String body = request("Default resolution", "TASK_RESOLUTION", "MANUAL");
        String created = mockMvc.perform(post("/v1/projects/{projectId}/ai-workflow-templates", PROJECT_ID)
                        .with(jwt(OWNER)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.name").value("Default resolution"))
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        mockMvc.perform(get("/v1/projects/{projectId}/ai-workflow-templates", PROJECT_ID).with(jwt(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get("/v1/ai-workflow-templates/{templateId}", id).with(jwt(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.workflowType").value("TASK_RESOLUTION"));

        mockMvc.perform(put("/v1/ai-workflow-templates/{templateId}", id).with(jwt(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content(request("Bug triage", "BUG_TRIAGE", "ADMIN_ONLY")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Bug triage"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(delete("/v1/ai-workflow-templates/{templateId}", id).with(jwt(OWNER)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/projects/{projectId}/ai-workflow-templates", PROJECT_ID).with(jwt(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/v1/ai-workflow-templates/{templateId}", id).with(jwt(MEMBER)))
                .andExpect(status().isNotFound());
    }

    @Test
    void authzAndOptimisticLockingAreEnforced() throws Exception {
        mockMvc.perform(get("/v1/projects/{projectId}/ai-workflow-templates", PROJECT_ID).with(jwt(OUTSIDER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/v1/projects/{projectId}/ai-workflow-templates", PROJECT_ID).with(jwt(MEMBER))
                        .contentType(MediaType.APPLICATION_JSON).content(request("PR", "PR_REVIEW", "MANUAL")))
                .andExpect(status().isForbidden());

        String created = mockMvc.perform(post("/v1/projects/{projectId}/ai-workflow-templates", PROJECT_ID)
                        .with(jwt(OWNER)).contentType(MediaType.APPLICATION_JSON).content(request("PR", "PR_REVIEW", "MANUAL")))
                .andReturn().getResponse().getContentAsString();
        String id = com.jayway.jsonpath.JsonPath.read(created, "$.id");
        mockMvc.perform(put("/v1/ai-workflow-templates/{templateId}", id).with(jwt(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content(staleRequest()))
                .andExpect(status().isConflict());
    }

    private String staleRequest() {
        return """
                {"name":"Stale","description":"Desc","workflowType":"PR_REVIEW","templateBody":"Do it","allowedTools":"task.update","approvalPolicy":"MANUAL","defaultModelPolicy":"gpt-4.1","version":999}
                """;
    }

    private String request(String name, String type, String approval) {
        return """
                {"name":"%s","description":"Desc","workflowType":"%s","templateBody":"Do it","allowedTools":"task.update","approvalPolicy":"%s","defaultModelPolicy":"gpt-4.1"}
                """.formatted(name, type, approval);
    }

    @TestConfiguration
    static class Fakes {
        @Bean InMemoryAiWorkflowTemplateRepository aiWorkflowTemplateRepository() { return new InMemoryAiWorkflowTemplateRepository(); }
        @Bean ProjectRepository projectRepository() { return new InMemoryProjectRepository(); }
        @Bean ProjectMembershipRepository projectMembershipRepository() { return new InMemoryMembershipRepository(); }
        @Bean AiWorkflowTemplateApplicationService service(AiWorkflowTemplateRepository t, ProjectRepository p, ProjectMembershipRepository m) { return new AiWorkflowTemplateApplicationService(t, p, m); }
    }

    static class InMemoryAiWorkflowTemplateRepository implements AiWorkflowTemplateRepository {
        final Map<UUID, AiWorkflowTemplate> store = new LinkedHashMap<>();
        void reset() { store.clear(); }
        public AiWorkflowTemplate save(AiWorkflowTemplate template) {
            long version = template.version() == null ? 0 : template.version() + 1;
            AiWorkflowTemplate saved = new AiWorkflowTemplate(template.id(), version, template.projectId(), template.name(), template.description(), template.workflowType(), template.templateBody(), template.allowedTools(), template.approvalPolicy(), template.defaultModelPolicy(), template.archivedAt(), template.createdAt(), template.updatedAt());
            store.put(saved.id(), saved); return saved;
        }
        public Optional<AiWorkflowTemplate> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        public List<AiWorkflowTemplate> findActiveByProjectId(UUID projectId) { return store.values().stream().filter(t -> t.projectId().equals(projectId) && !t.archived()).toList(); }
    }
    static class InMemoryProjectRepository implements ProjectRepository {
        public Project save(Project project) { return project; }
        public Optional<Project> findById(UUID id) { return Optional.of(new Project(PROJECT_ID, 0L, "P", "PRJ", null, UUID.fromString(OWNER), null, Instant.now(), Instant.now())).filter(p -> p.id().equals(id)); }
        public Optional<Project> findByIdForUpdate(UUID id) { return findById(id); }
        public List<Project> findAll() { return List.of(); }
        public boolean existsByKey(String key) { return false; }
    }
    static class InMemoryMembershipRepository implements ProjectMembershipRepository {
        public ProjectMembership save(ProjectMembership membership) { return membership; }
        public void deleteByProjectIdAndUserId(UUID projectId, UUID userId) {}
        public List<ProjectMembership> findByProjectId(UUID projectId) { return List.of(); }
        public Optional<ProjectMembership> findByProjectIdAndUserId(UUID projectId, UUID userId) { return existsByProjectIdAndUserId(projectId, userId) ? Optional.of(new ProjectMembership(projectId, userId, ProjectMembershipRole.MEMBER)) : Optional.empty(); }
        public boolean existsByProjectIdAndUserId(UUID projectId, UUID userId) { return projectId.equals(PROJECT_ID) && userId.equals(UUID.fromString(MEMBER)); }
    }
}
