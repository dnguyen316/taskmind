package com.taskmind.ai.security;

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
    }
}
