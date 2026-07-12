package com.taskmind.ai.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NovaSecurityTest {
    @Autowired MockMvc mockMvc;

    @Test
    void healthIsPublicAndInternalRoutesRequireServiceToken() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("nova-ai"));
        mockMvc.perform(get("/internal/ai/capabilities")).andExpect(status().isForbidden());
        mockMvc.perform(get("/internal/ai/capabilities").header("X-Service-Token", "wrong"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/internal/ai/capabilities").header("X-Service-Token", "test-ai-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capabilities").isArray());
        mockMvc.perform(get("/internal/ai/capabilities").header("Authorization", "Bearer test-ai-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capabilities").isArray());
    }

    @Test
    void prometheusScrapeIsPublicButOtherActuatorEndpointsRemainDenied() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
        mockMvc.perform(get("/actuator/health")).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isForbidden());
    }
}
