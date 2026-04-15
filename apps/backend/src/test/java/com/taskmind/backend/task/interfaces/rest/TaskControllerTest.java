package com.taskmind.backend.task.interfaces.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsTask() throws Exception {
        var payload = """
            {
              "userId": "11111111-1111-1111-1111-111111111111",
              "title": "Write backend APIs",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """;

        mockMvc.perform(post("/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Write backend APIs"))
            .andExpect(jsonPath("$.status").value("TODO"))
            .andExpect(jsonPath("$.priority").value(2));
    }

    @Test
    void rejectsInvalidPriority() throws Exception {
        var payload = """
            {
              "userId": "11111111-1111-1111-1111-111111111111",
              "title": "Invalid priority",
              "status": "TODO",
              "priority": 9,
              "source": "MANUAL"
            }
            """;

        mockMvc.perform(post("/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updatesExistingTask() throws Exception {
        var createPayload = """
            {
              "userId": "11111111-1111-1111-1111-111111111111",
              "title": "Task to patch",
              "status": "TODO",
              "priority": 3,
              "source": "MANUAL"
            }
            """;

        var createResponse = mockMvc.perform(post("/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andReturn();

        var responseBody = createResponse.getResponse().getContentAsString();
        var id = objectMapper.readTree(responseBody).get("id").asText();

        var patchPayload = """
            {
              "status": "IN_PROGRESS",
              "priority": 1
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(patchPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.priority").value(1));
    }

    @Test
    void updatesTaskStatusAndChecksCompletion() throws Exception {
        var createPayload = """
            {
              "userId": "11111111-1111-1111-1111-111111111111",
              "title": "Check completion",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """;

        var createResponse = mockMvc.perform(post("/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andReturn();

        var id = objectMapper.readTree(createResponse.getResponse().getContentAsString()).get("id").asText();

        var statusPayload = """
            {
              "status": "DONE"
            }
            """;

        mockMvc.perform(patch("/v1/tasks/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(statusPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(get("/v1/tasks/{id}/completion", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.completed").value(true))
            .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void filtersTasksByUserId() throws Exception {
        var payloadA = """
            {
              "userId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
              "title": "User A task",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """;

        var payloadB = """
            {
              "userId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
              "title": "User B task",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """;

        mockMvc.perform(post("/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(payloadA))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(payloadB))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/tasks").queryParam("userId", "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
    }

    @Test
    void filtersByStatusAndPagination() throws Exception {
        var todoPayload = """
            {
              "userId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
              "title": "Todo task",
              "status": "TODO",
              "priority": 2,
              "source": "MANUAL"
            }
            """;
        var donePayload = """
            {
              "userId": "cccccccc-cccc-cccc-cccc-cccccccccccc",
              "title": "Done task",
              "status": "DONE",
              "priority": 2,
              "source": "MANUAL"
            }
            """;

        mockMvc.perform(post("/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(todoPayload))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(donePayload))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/tasks")
                .queryParam("userId", "cccccccc-cccc-cccc-cccc-cccccccccccc")
                .queryParam("status", "TODO")
                .queryParam("page", "0")
                .queryParam("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("TODO"));
    }

    @Test
    void filtersOverdueAndArchivesTask() throws Exception {
        var overdueDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1).toString();
        var createPayload = """
            {
              "userId": "dddddddd-dddd-dddd-dddd-dddddddddddd",
              "title": "Overdue task",
              "status": "TODO",
              "priority": 2,
              "dueAt": "%s",
              "source": "MANUAL"
            }
            """.formatted(overdueDate);

        var createResponse = mockMvc.perform(post("/v1/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andReturn();

        var id = objectMapper.readTree(createResponse.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/v1/tasks")
                .queryParam("userId", "dddddddd-dddd-dddd-dddd-dddddddddddd")
                .queryParam("overdueOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(patch("/v1/tasks/{id}/archive", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(get("/v1/tasks")
                .queryParam("userId", "dddddddd-dddd-dddd-dddd-dddddddddddd")
                .queryParam("overdueOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }
}
