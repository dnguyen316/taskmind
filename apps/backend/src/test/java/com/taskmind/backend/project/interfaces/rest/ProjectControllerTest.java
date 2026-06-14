package com.taskmind.backend.project.interfaces.rest;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    private String projectKey() {
        return java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
