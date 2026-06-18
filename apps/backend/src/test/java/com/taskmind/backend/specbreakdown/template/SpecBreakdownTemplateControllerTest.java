package com.taskmind.backend.specbreakdown.template;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskmind.backend.security.AuthenticatedUserResolver;
import com.taskmind.backend.security.JwtClaimAuthenticationConverter;
import com.taskmind.backend.security.SecurityConfig;
import com.taskmind.backend.security.TestJwtSupport;
import com.taskmind.backend.specbreakdown.application.SpecBreakdownTemplateApplicationService;
import com.taskmind.backend.specbreakdown.domain.repository.SpecBreakdownTemplateRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = SpecBreakdownTemplateController.class)
@Import({
    SecurityConfig.class,
    AuthenticatedUserResolver.class,
    JwtClaimAuthenticationConverter.class,
    TestJwtSupport.Config.class,
    SpecBreakdownTemplateControllerTest.Fakes.class
})
class SpecBreakdownTemplateControllerTest {
    private static final String USER_ID = "11111111-1111-1111-1111-111111111111";
    private static final UUID PROJECT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Autowired MockMvc mockMvc;

    @Test
    void createsListsUpdatesAndDeletesTemplates() throws Exception {
        String created = mockMvc.perform(post("/v1/projects/{projectId}/spec-templates", PROJECT_ID)
                        .with(jwt(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {"name":"Default breakdown","description":"Default fields","fields":"{\\"issueType\\":\\"Story\\"}"}
            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.name").value("Default breakdown"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        mockMvc.perform(get("/v1/projects/{projectId}/spec-templates", PROJECT_ID).with(jwt(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(put("/v1/spec-templates/{id}", id)
                        .with(jwt(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
            {"name":"Updated breakdown","description":"Updated","fields":"{\\"issueType\\":\\"Task\\"}"}
            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated breakdown"))
                .andExpect(jsonPath("$.fields").value("{\"issueType\":\"Task\"}"));

        mockMvc.perform(delete("/v1/spec-templates/{id}", id).with(jwt(USER_ID)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/projects/{projectId}/spec-templates", PROJECT_ID).with(jwt(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void returnsNotFoundForMissingUpdateAndDelete() throws Exception {
        UUID missing = UUID.fromString("99999999-9999-9999-9999-999999999999");

        mockMvc.perform(put("/v1/spec-templates/{id}", missing)
                        .with(jwt(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Missing\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/v1/spec-templates/{id}", missing).with(jwt(USER_ID)))
                .andExpect(status().isNotFound());
    }

    @TestConfiguration
    static class Fakes {
        @Bean
        InMemorySpecBreakdownTemplateRepository specBreakdownTemplateRepository() {
            return new InMemorySpecBreakdownTemplateRepository();
        }

        @Bean
        SpecBreakdownTemplateApplicationService specBreakdownTemplateApplicationService(
                SpecBreakdownTemplateRepository repository) {
            return new SpecBreakdownTemplateApplicationService(repository);
        }
    }
}
