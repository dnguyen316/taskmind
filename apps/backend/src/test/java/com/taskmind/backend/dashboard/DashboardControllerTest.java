package com.taskmind.backend.dashboard;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.taskmind.backend.relay.RelayClientException;
import com.taskmind.backend.relay.RelayContextPort;
import java.time.OffsetDateTime;
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
class DashboardControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean RelayContextPort relay;

    @Test
    void returnsFrontendCompatibleAggregation() throws Exception {
        UUID task = UUID.randomUUID(), project = UUID.randomUUID();
        when(relay.userTasks(any()))
                .thenReturn(
                        List.of(
                                Map.of(
                                        "task_id",
                                        task,
                                        "project_id",
                                        project,
                                        "title",
                                        "Ship reports",
                                        "status",
                                        "TODO",
                                        "updated_at",
                                        OffsetDateTime.parse("2026-01-01T00:00:00Z"))));
        when(relay.projectMetrics(any()))
                .thenReturn(
                        List.of(
                                Map.of(
                                        "metric_date",
                                        java.sql.Date.valueOf("2026-01-01"),
                                        "tasks_created",
                                        2,
                                        "tasks_completed",
                                        1,
                                        "events_ingested",
                                        3)));
        mockMvc.perform(get("/v1/dashboard").with(jwt("11111111-1111-1111-1111-111111111111")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.openTasks").value(1))
                .andExpect(jsonPath("$.myTasks[0].title").value("Ship reports"))
                .andExpect(jsonPath("$.activity[0].eventsIngested").value(3));
    }

    @Test
    void returnsEmptyState() throws Exception {
        when(relay.userTasks(any())).thenReturn(List.of());
        when(relay.projectMetrics(any())).thenReturn(List.of());
        mockMvc.perform(get("/v1/dashboard").with(jwt("11111111-1111-1111-1111-111111111111")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpis.openTasks").value(0))
                .andExpect(jsonPath("$.myTasks", empty()))
                .andExpect(jsonPath("$.activity", empty()));
    }

    @Test
    void relayFailureIsServiceUnavailable() throws Exception {
        when(relay.userTasks(any()))
                .thenThrow(
                        new RelayClientException(
                                "Relay context API is unavailable", new RuntimeException()));
        mockMvc.perform(get("/v1/dashboard").with(jwt("11111111-1111-1111-1111-111111111111")))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/v1/dashboard")).andExpect(status().isUnauthorized());
    }
}
