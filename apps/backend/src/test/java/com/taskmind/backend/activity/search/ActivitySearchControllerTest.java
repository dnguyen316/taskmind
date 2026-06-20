package com.taskmind.backend.activity.search;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class ActivitySearchControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void disabledSearchFailsPredictably() throws Exception {
        mockMvc.perform(
                        get("/v1/activity/search")
                                .with(jwt("11111111-1111-1111-1111-111111111111"))
                                .queryParam("q", "task"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void disabledSuggestionsFailPredictably() throws Exception {
        mockMvc.perform(
                        get("/v1/activity/search/suggest")
                                .with(jwt("11111111-1111-1111-1111-111111111111"))
                                .queryParam("q", "task"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void rejectsInvalidStructuredFilters() throws Exception {
        mockMvc.perform(
                        get("/v1/activity/search")
                                .with(jwt("11111111-1111-1111-1111-111111111111"))
                                .queryParam("projectId", "not-a-uuid"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        get("/v1/activity/search")
                                .with(jwt("11111111-1111-1111-1111-111111111111"))
                                .queryParam("from", "2026-02-01T00:00:00Z")
                                .queryParam("to", "2026-01-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnauthenticatedStructuredFilterSearch() throws Exception {
        mockMvc.perform(get("/v1/activity/search").queryParam("entityType", "task"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/v1/activity/search")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/v1/activity/search/suggest")).andExpect(status().isUnauthorized());
    }
}
