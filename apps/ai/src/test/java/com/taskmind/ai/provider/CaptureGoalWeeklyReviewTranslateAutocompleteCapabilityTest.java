package com.taskmind.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaptureGoalWeeklyReviewTranslateAutocompleteCapabilityTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockAiProvider provider = new MockAiProvider("test-model", objectMapper);

    @Test
    void captureGoalWeeklyReviewTranslateAndAutocompleteReturnTypedDeterministicShapes() {
        assertThat(run(AiCapabilityId.CAPTURE, "text", "Plan launch").get("drafts")).hasSize(1);
        assertThat(
                        run(
                                        AiCapabilityId.GOAL_BREAKDOWN,
                                        "goalId",
                                        "00000000-0000-0000-0000-000000000001")
                                .get("tasks"))
                .hasSize(1);
        assertThat(
                        run(
                                        AiCapabilityId.WEEKLY_REVIEW,
                                        "userId",
                                        "00000000-0000-0000-0000-000000000002")
                                .get("recommendations"))
                .hasSize(1);
        assertThat(run(AiCapabilityId.AUTOCOMPLETE_TASK, "text", "Write docs").get("suggestions"))
                .hasSize(2);
        assertThat(
                        run(AiCapabilityId.TRANSLATE_TASK, "text", "Write docs")
                                .get("translatedText")
                                .asText())
                .contains("Write docs");
    }

    private com.fasterxml.jackson.databind.JsonNode run(
            AiCapabilityId capabilityId, String field, String value) {
        return provider.complete(
                        new ProviderRequest(
                                capabilityId,
                                objectMapper.createObjectNode().put(field, value),
                                List.of(),
                                "corr"))
                .output();
    }
}
