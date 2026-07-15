package com.taskmind.backend.task.interfaces.rest;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmind.backend.config.logging.RequestCorrelation;
import com.taskmind.backend.project.domain.model.Project;
import com.taskmind.backend.project.domain.repository.ProjectRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(com.taskmind.backend.security.TestJwtSupport.Config.class)
class TaskReleaseControllerTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID NON_MEMBER_ID = UUID.fromString("66666666-7777-8888-9999-000000000000");
    private static final String CORRELATION_ID = "release-summary-correlation";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectRepository projects;

    @Test
    void missingProjectReturnsNotFoundProblemWithStableCodeAndCorrelationMetadata() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(get("/v1/projects/{projectId}/releases", projectId)
                        .with(jwt(OWNER_ID.toString()))
                        .header(RequestCorrelation.HEADER_NAME, CORRELATION_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"))
                .andExpect(jsonPath("$.resource").value("project"))
                .andExpect(jsonPath("$.resourceId").value(projectId.toString()))
                .andExpect(jsonPath("$.operation").value("release-summary"))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
    }

    @Test
    void nonMemberAccessReturnsForbiddenProblemWithStableCodeAndCorrelationMetadata() throws Exception {
        Project project = createProject(OWNER_ID);

        mockMvc.perform(get("/v1/projects/{projectId}/releases", project.id())
                        .with(jwt(NON_MEMBER_ID.toString()))
                        .header(RequestCorrelation.HEADER_NAME, CORRELATION_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TASK_ACCESS_DENIED"))
                .andExpect(jsonPath("$.resource").value("project"))
                .andExpect(jsonPath("$.resourceId").doesNotExist())
                .andExpect(jsonPath("$.operation").value("release-summary"))
                .andExpect(jsonPath("$.correlationId").value(CORRELATION_ID));
    }

    private Project createProject(UUID ownerId) {
        Instant now = Instant.now();
        String key = "RS" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return projects.save(new Project(UUID.randomUUID(), null, key + " Project", key, null, ownerId, null, now, now));
    }
}
