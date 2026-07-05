package com.taskmind.backend.task.interfaces.rest;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class SavedTaskViewControllerTest {
    @Autowired private MockMvc mockMvc;

    @Test
    void createsAndListsOnlyOwnersSavedViews() throws Exception {
        mockMvc.perform(post("/v1/task-saved-views")
                .with(jwt("11111111-1111-1111-1111-111111111111"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"My overdue","filters":{"overdueOnly":true,"priority":1}}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("My overdue"))
            .andExpect(jsonPath("$.filters.overdueOnly").value(true));

        mockMvc.perform(get("/v1/task-saved-views").with(jwt("11111111-1111-1111-1111-111111111111")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/v1/task-saved-views").with(jwt("22222222-2222-2222-2222-222222222222")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
