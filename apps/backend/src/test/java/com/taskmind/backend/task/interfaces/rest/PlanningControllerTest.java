package com.taskmind.backend.task.interfaces.rest;

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
class PlanningControllerTest {

    private static final String REQUESTER_ID = "77777777-7777-7777-7777-777777777777";
    private static final String OTHER_USER_ID = "88888888-8888-8888-8888-888888888888";
    private static final String PRIVILEGED_TARGET_ID = "99999999-8888-8888-8888-888888888888";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void arbitraryRoleHeaderCannotElevateDailyPlannerAccess() throws Exception {
        createTask(OTHER_USER_ID, "Other user's task");

        mockMvc.perform(post("/v1/planner/daily/generate")
                .with(jwt(REQUESTER_ID))
                .header("X-User-Id", OTHER_USER_ID)
                .header("X-User-Roles", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "88888888-8888-8888-8888-888888888888",
                      "availableMinutes": 60,
                      "includeBlockedTasks": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plan.length()").value(0))
            .andExpect(jsonPath("$.overflow.length()").value(0));
    }

    @Test
    void validatedJwtRoleAllowsPrivilegedDailyPlannerAccess() throws Exception {
        createTask(PRIVILEGED_TARGET_ID, "Other user's privileged task");

        mockMvc.perform(post("/v1/planner/daily/generate")
                .with(jwt(REQUESTER_ID, "MANAGER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "99999999-8888-8888-8888-888888888888",
                      "availableMinutes": 60,
                      "includeBlockedTasks": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plan[0].title").value("Other user's privileged task"));
    }

    private void createTask(String userId, String title) throws Exception {
        mockMvc.perform(post("/v1/tasks")
                .with(jwt(userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "%s",
                      "title": "%s",
                      "status": "IN_PROGRESS",
                      "priority": 2,
                      "durationMinutes": 30,
                      "source": "MANUAL"
                    }
                    """.formatted(userId, title)))
            .andExpect(status().isCreated());
    }
}
