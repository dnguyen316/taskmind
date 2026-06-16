package com.taskmind.ai.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.provider.MockAiProvider;
import com.taskmind.ai.provider.ProviderRequest;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TypedCapabilitiesTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockAiProvider provider = new MockAiProvider("test-model", objectMapper);

    @ParameterizedTest
    @MethodSource("capabilities")
    void validatesRequiredInput(
            Capability capability, JsonNode validInput, List<String> expectedOutputFields) {
        assertThat(capability.buildProviderInput(validInput)).isEqualTo(validInput);

        assertThatThrownBy(() -> capability.buildProviderInput(objectMapper.createObjectNode()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(capability.id().value());
    }

    @ParameterizedTest
    @MethodSource("capabilities")
    void declaresAndProducesTypedOutputShape(
            Capability capability, JsonNode validInput, List<String> expectedOutputFields) {
        assertThat(capability.outputSchema().get("additionalProperties").asBoolean()).isFalse();
        expectedOutputFields.forEach(
                field ->
                        assertThat(capability.outputSchema().get("required").toString())
                                .contains(field));

        JsonNode output =
                provider.complete(
                                new ProviderRequest(capability.id(), validInput, List.of(), "corr"))
                        .output();

        expectedOutputFields.forEach(field -> assertThat(output.has(field)).as(field).isTrue());
    }

    static Stream<Arguments> capabilities() {
        ObjectMapper mapper = new ObjectMapper();
        return Stream.of(
                Arguments.of(
                        new CaptureCapability(mapper),
                        mapper.createObjectNode().put("text", "Plan launch"),
                        List.of("drafts", "clarificationQuestion")),
                Arguments.of(
                        new DescribeTaskCapability(mapper),
                        mapper.createObjectNode().put("title", "Write docs"),
                        List.of("description", "rationale")),
                Arguments.of(
                        new AutocompleteTaskCapability(mapper),
                        mapper.createObjectNode().put("text", "Write docs"),
                        List.of("suggestions")),
                Arguments.of(
                        new TranslateTaskCapability(mapper),
                        mapper.createObjectNode()
                                .put("text", "Write docs")
                                .put("targetLanguage", "Spanish"),
                        List.of("translatedText", "targetLanguage")),
                Arguments.of(
                        new WeeklyReviewCapability(mapper),
                        mapper.createObjectNode()
                                .put("userId", "00000000-0000-0000-0000-000000000001"),
                        List.of(
                                "userId",
                                "summary",
                                "slippageInsights",
                                "recommendations",
                                "nextWeekPriorities")),
                Arguments.of(
                        new GoalBreakdownCapability(mapper),
                        mapper.createObjectNode()
                                .put("goalId", "00000000-0000-0000-0000-000000000002"),
                        List.of("goalId", "milestones", "tasks", "riskNotes")),
                Arguments.of(
                        new ProjectBriefCapability(mapper),
                        mapper.createObjectNode()
                                .put("projectId", "00000000-0000-0000-0000-000000000003"),
                        List.of(
                                "projectId",
                                "summary",
                                "currentFocus",
                                "risks",
                                "suggestedNextSteps")),
                Arguments.of(
                        new DurationEstimateCapability(mapper),
                        mapper.createObjectNode().put("title", "Write docs"),
                        List.of("durationMinutes", "rationale", "confidence")),
                Arguments.of(
                        new RationalePhraseCapability(mapper),
                        mapper.createObjectNode().put("title", "Write docs"),
                        List.of("rationale")),
                Arguments.of(
                        new DashboardInsightsCapability(mapper),
                        mapper.createObjectNode()
                                .put("userId", "00000000-0000-0000-0000-000000000004"),
                        List.of("userId", "summary", "insights", "recommendations")),
                Arguments.of(
                        new SpecOutlineCapability(mapper),
                        mapper.createObjectNode().put("specText", "Launch reminders"),
                        List.of("epics", "stories", "warnings")),
                Arguments.of(
                        new SpecEnrichCapability(mapper),
                        mapper.createObjectNode().put("specText", "Launch reminders"),
                        List.of("items", "risks", "labels")),
                Arguments.of(
                        new SpecBreakdownCapability(mapper),
                        mapper.createObjectNode().put("specText", "Launch reminders"),
                        List.of("tree", "metadata", "warnings")),
                Arguments.of(
                        new SpecBreakdownSectionCapability(mapper),
                        mapper.createObjectNode().put("sectionText", "Reminder preferences"),
                        List.of("sectionTitle", "items", "warnings")),
                Arguments.of(
                        new SpecBreakdownMergeCapability(mapper),
                        mapper.createObjectNode().put("draftTree", "[]"),
                        List.of("mergedTree", "conflicts", "warnings")),
                Arguments.of(
                        new SpecSuggestLinksCapability(mapper),
                        mapper.createObjectNode().put("specText", "Launch reminders"),
                        List.of("links", "dependencies", "warnings")));
    }
}
