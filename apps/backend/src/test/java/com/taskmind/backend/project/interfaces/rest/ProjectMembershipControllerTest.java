package com.taskmind.backend.project.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ProjectMembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addsListsAndRemovesProjectMember() throws Exception {
        var projectPayload = """
            {
              "name": "Membership test project",
              "key": "MTP",
              "ownerUserId": "11111111-1111-1111-1111-111111111111"
            }
            """;

        var createProjectResponse = mockMvc.perform(post("/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(projectPayload))
            .andExpect(status().isCreated())
            .andReturn();

        var projectId = objectMapper.readTree(createProjectResponse.getResponse().getContentAsString()).get("id").asText();

        var addMemberPayload = """
            {
              "userId": "22222222-2222-2222-2222-222222222222",
              "role": "MEMBER"
            }
            """;

        mockMvc.perform(post("/v1/projects/{projectId}/members", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(addMemberPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.projectId").value(projectId))
            .andExpect(jsonPath("$.userId").value("22222222-2222-2222-2222-222222222222"))
            .andExpect(jsonPath("$.role").value("MEMBER"));

        mockMvc.perform(get("/v1/projects/{projectId}/members", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userId").value("22222222-2222-2222-2222-222222222222"));

        mockMvc.perform(delete("/v1/projects/{projectId}/members/{userId}", projectId, "22222222-2222-2222-2222-222222222222"))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/projects/{projectId}/members", projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
