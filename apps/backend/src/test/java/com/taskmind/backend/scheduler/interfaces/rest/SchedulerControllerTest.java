package com.taskmind.backend.scheduler.interfaces.rest;

import static com.taskmind.backend.security.TestJwtSupport.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(com.taskmind.backend.security.TestJwtSupport.Config.class)
class SchedulerControllerTest {
    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @Test
    void getsAndUpdatesSchedulingPreferences() throws Exception {
        var userId = "12121212-1212-1212-1212-121212121212";

        mockMvc.perform(get("/v1/scheduler/preferences").with(jwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workdayStart").value("09:00:00"))
                .andExpect(jsonPath("$.blockGranularityMinutes").value(30));

        var payload =
                """
            {
              "workdayStart": "08:30:00",
              "workdayEnd": "15:30:00",
              "blockGranularityMinutes": 30,
              "maxDailyFocusMinutes": 240
            }
            """;

        mockMvc.perform(
                        put("/v1/scheduler/preferences")
                                .with(jwt(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workdayStart").value("08:30:00"))
                .andExpect(jsonPath("$.maxDailyFocusMinutes").value(240));
    }

    @Test
    void generatesListsPatchesAndCompletesBlocks() throws Exception {
        var userId = "13131313-1313-1313-1313-131313131313";
        var taskResponse =
                mockMvc.perform(
                                post("/v1/tasks")
                                        .with(jwt(userId))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                    {"userId":"13131313-1313-1313-1313-131313131313","title":"Schedule me","status":"TODO","priority":1,"durationMinutes":60,"source":"MANUAL"}
                    """))
                        .andExpect(status().isCreated())
                        .andReturn();
        var taskId =
                objectMapper
                        .readTree(taskResponse.getResponse().getContentAsString())
                        .get("id")
                        .asText();

        var generated =
                mockMvc.perform(
                                post("/v1/scheduler/generate")
                                        .with(jwt(userId))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                    {"from":"2026-06-08T08:00:00Z","to":"2026-06-09T18:00:00Z"}
                    """))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.blocks.length()").value(1))
                        .andExpect(jsonPath("$.blocks[0].taskId").value(taskId))
                        .andReturn();
        var block =
                objectMapper
                        .readTree(generated.getResponse().getContentAsString())
                        .get("blocks")
                        .get(0);
        var blockId = block.get("id").asText();

        mockMvc.perform(
                        get("/v1/scheduler/blocks")
                                .with(jwt(userId))
                                .queryParam("from", "2026-06-08T00:00:00Z")
                                .queryParam("to", "2026-06-10T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(blockId));

        mockMvc.perform(
                        patch("/v1/scheduler/blocks/{id}", blockId)
                                .with(jwt(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"startsAt":"2026-06-08T10:00:00Z","endsAt":"2026-06-08T11:00:00Z","rationale":"Manual adjustment"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rationale").value("Manual adjustment"));

        mockMvc.perform(post("/v1/scheduler/blocks/{id}/complete", blockId).with(jwt(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void rejectsStaleScheduledBlockUpdateWithConflict() throws Exception {
        var userId = "14141414-1414-1414-1414-141414141414";
        mockMvc.perform(
                        post("/v1/tasks")
                                .with(jwt(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"userId":"14141414-1414-1414-1414-141414141414","title":"Versioned schedule","status":"TODO","priority":1,"durationMinutes":60,"source":"MANUAL"}
                    """))
                .andExpect(status().isCreated());
        var generated =
                mockMvc.perform(
                                post("/v1/scheduler/generate")
                                        .with(jwt(userId))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{"
                                                        + "\"from\":\"2026-06-08T08:00:00Z\",\"to\":\"2026-06-09T18:00:00Z\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        var blockId =
                objectMapper
                        .readTree(generated.getResponse().getContentAsString())
                        .get("blocks")
                        .get(0)
                        .get("id")
                        .asText();

        mockMvc.perform(
                        patch("/v1/scheduler/blocks/{id}", blockId)
                                .with(jwt(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"version\":999,\"rationale\":\"stale\"}"))
                .andExpect(status().isConflict());
    }
    @Test
    void schedulerGenerateOwnsPlanningBehaviorAndPersistsWithoutDuplicates() throws Exception {
        var userId = "15151515-1515-1515-1515-151515151515";
        mockMvc.perform(
                        post("/v1/tasks")
                                .with(jwt(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"userId":"15151515-1515-1515-1515-151515151515","title":"Already started","status":"IN_PROGRESS","priority":1,"durationMinutes":30,"source":"MANUAL"}
                    """))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/v1/scheduler/generate")
                                .with(jwt(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"from":"2026-06-15T08:00:00Z","to":"2026-06-16T18:00:00Z"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks.length()").value(1))
                .andExpect(jsonPath("$.blocks[0].startsAt").value("2026-06-15T09:00:00Z"));

        mockMvc.perform(
                        post("/v1/scheduler/generate")
                                .with(jwt(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"from":"2026-06-15T08:00:00Z","to":"2026-06-16T18:00:00Z"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks.length()").value(0));

        mockMvc.perform(
                        get("/v1/scheduler/blocks")
                                .with(jwt(userId))
                                .queryParam("from", "2026-06-15T00:00:00Z")
                                .queryParam("to", "2026-06-17T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void schedulerGenerateMarksMissedBlocksAndReturnsRescheduleProposals() throws Exception {
        var userId = "16161616-1616-1616-1616-161616161616";
        mockMvc.perform(
                        post("/v1/tasks")
                                .with(jwt(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"userId":"16161616-1616-1616-1616-161616161616","title":"Missed schedule","status":"TODO","priority":1,"durationMinutes":30,"source":"MANUAL"}
                    """))
                .andExpect(status().isCreated());

        OffsetDateTime windowStart = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2).withHour(8).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime windowEnd = windowStart.plusDays(1).withHour(18);

        mockMvc.perform(
                        post("/v1/scheduler/generate")
                                .with(jwt(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"from":"%s","to":"%s"}
                    """
                                                .formatted(windowStart, windowEnd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks.length()").value(1))
                .andExpect(jsonPath("$.proposals.length()").value(1))
                .andExpect(jsonPath("$.proposals[0].reason").value("Block is overdue and should be moved"));

        mockMvc.perform(
                        get("/v1/scheduler/blocks")
                                .with(jwt(userId))
                                .queryParam("from", windowStart.minusDays(1).toString())
                                .queryParam("to", windowEnd.plusDays(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("MISSED"));
    }

    @Test
    void rejectsStalePreferenceUpdateWithConflict() throws Exception {
        var userId = "17171717-1717-1717-1717-171717171717";

        mockMvc.perform(get("/v1/scheduler/preferences").with(jwt(userId)))
                .andExpect(status().isOk());

        mockMvc.perform(
                        put("/v1/scheduler/preferences")
                                .with(jwt(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"version":999,"workdayStart":"08:00:00","workdayEnd":"16:00:00","blockGranularityMinutes":30,"maxDailyFocusMinutes":240}
                    """))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsInvalidScheduleGenerationWindow() throws Exception {
        mockMvc.perform(
                        post("/v1/scheduler/generate")
                                .with(jwt("18181818-1818-1818-1818-181818181818"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                    {"from":"2026-06-09T08:00:00Z","to":"2026-06-08T18:00:00Z"}
                    """))
                .andExpect(status().isBadRequest());
    }

}
