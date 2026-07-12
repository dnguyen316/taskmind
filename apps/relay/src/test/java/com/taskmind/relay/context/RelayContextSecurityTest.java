package com.taskmind.relay.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmind.relay.ingest.RelayTestSchema;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RelayContextSecurityTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        RelayTestSchema.create(jdbcTemplate);
    }

    @Test
    void internalContextRequiresServiceToken() throws Exception {
        mockMvc.perform(get("/internal/context/users/" + UUID.randomUUID() + "/tasks"))
                .andExpect(status().isForbidden());
        mockMvc.perform(
                        get("/internal/context/users/" + UUID.randomUUID() + "/tasks")
                                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());
    }

    @Test
    void prometheusScrapeIsPublicButOtherActuatorEndpointsRemainDenied() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(403));
        mockMvc.perform(get("/actuator/health")).andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isForbidden());
    }
}
