package com.taskmind.backend.team;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.taskmind.backend.analytics.application.AnalyticsRollupRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(com.taskmind.backend.security.TestJwtSupport.Config.class)
class TeamControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean AnalyticsRollupRepository analytics;

    @Test
    void privilegedUserGetsEmptyDirectory() throws Exception {
        when(analytics.assigneeWorkload()).thenReturn(List.of());
        mockMvc.perform(
                        get("/v1/team/directory")
                                .with(jwt("11111111-1111-1111-1111-111111111111", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembers").value(1))
                .andExpect(jsonPath("$.members").isArray());
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
}
