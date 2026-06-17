package com.taskmind.backend.analytics;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.taskmind.backend.analytics.application.*;
import com.taskmind.backend.analytics.application.AnalyticsRollupRepository;
import java.time.LocalDate;
import java.util.*;
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
class AnalyticsControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean AnalyticsRollupRepository repo;

    @Test
    void returnsReportsShape() throws Exception {
        when(repo.userTrends(any(), any()))
                .thenReturn(List.of(new ReportsTrend(LocalDate.parse("2026-01-01"), 3, 2, 5)));
        when(repo.statusSegments(any())).thenReturn(List.of(new ReportsStatusSegment("TODO", 1)));
        when(repo.projectThroughput(any()))
                .thenReturn(List.of(new ReportsProjectThroughput(UUID.randomUUID(), "Core", 3, 2)));
        when(repo.assigneeWorkload()).thenReturn(List.of());
        mockMvc.perform(
                        get("/v1/reports?range=month")
                                .with(jwt("11111111-1111-1111-1111-111111111111")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range").value("MONTH"))
                .andExpect(jsonPath("$.kpis.tasksCreated").value(3))
                .andExpect(jsonPath("$.sparklines.tasksCompleted[0]").value(2))
                .andExpect(jsonPath("$.statusSegments[0].status").value("TODO"))
                .andExpect(jsonPath("$.projectThroughput[0].name").value("Core"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/v1/reports")).andExpect(status().isUnauthorized());
    }
}
