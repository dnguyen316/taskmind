package com.taskmind.ai.capability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
public class CapabilityRegistry {
    private final Map<AiCapabilityId, Capability> capabilities;

    public CapabilityRegistry(List<Capability> capabilities) {
        this.capabilities =
                capabilities.stream()
                        .collect(Collectors.toUnmodifiableMap(Capability::id, Function.identity()));
    }

    public Optional<Capability> find(AiCapabilityId id) {
        return Optional.ofNullable(capabilities.get(id));
    }

    public List<CapabilityDescriptor> list() {
        return capabilities.values().stream()
                .sorted(Comparator.comparing(capability -> capability.id().value()))
                .map(
                        capability ->
                                new CapabilityDescriptor(
                                        capability.id().value(),
                                        capability.description(),
                                        capability.inputSchema(),
                                        capability.outputSchema()))
                .toList();
    }

    @Configuration
    static class CapabilityConfiguration {
        @Bean
        Capability captureCapability(ObjectMapper objectMapper) {
            return new PlaceholderCapability(
                    AiCapabilityId.CAPTURE,
                    "Draft a structured capture proposal from user input.",
                    objectMapper);
        }


        @Bean
        Capability goalBreakdownCapability(ObjectMapper objectMapper) {
            return new PlaceholderCapability(
                    AiCapabilityId.GOAL_BREAKDOWN,
                    "Break a goal into milestones and draft tasks.",
                    objectMapper);
        }

        @Bean
        Capability weeklyReviewCapability(ObjectMapper objectMapper) {
            return new PlaceholderCapability(
                    AiCapabilityId.WEEKLY_REVIEW,
                    "Draft weekly review insights from context.",
                    objectMapper);
        }


        @Bean
        Capability projectBriefCapability(ObjectMapper objectMapper) {
            return new PlaceholderCapability(
                    AiCapabilityId.PROJECT_BRIEF,
                    "Generate a concise project brief from project state.",
                    objectMapper);
        }

        @Bean
        Capability describeTaskCapability(ObjectMapper objectMapper) {
            return new PlaceholderCapability(
                    AiCapabilityId.DESCRIBE_TASK,
                    "Draft a task description from a title and notes.",
                    objectMapper);
        }

        @Bean
        Capability autocompleteTaskCapability(ObjectMapper objectMapper) {
            return new PlaceholderCapability(
                    AiCapabilityId.AUTOCOMPLETE_TASK,
                    "Suggest task description completions.",
                    objectMapper);
        }

        @Bean
        Capability translateTaskCapability(ObjectMapper objectMapper) {
            return new PlaceholderCapability(
                    AiCapabilityId.TRANSLATE_TASK,
                    "Translate task text into a requested language.",
                    objectMapper);
        }

        @Bean
        Capability durationEstimateCapability(ObjectMapper objectMapper) {
            return new PlaceholderCapability(
                    AiCapabilityId.DURATION_ESTIMATE,
                    "Estimate task duration with rationale.",
                    objectMapper);
        }

        @Bean
        Capability rationalePhraseCapability(ObjectMapper objectMapper) {
            return new PlaceholderCapability(
                    AiCapabilityId.RATIONALE_PHRASE,
                    "Generate a short schedule rationale phrase.",
                    objectMapper);
        }

        @Bean
        Capability dashboardInsightsCapability(ObjectMapper objectMapper) {
            return new PlaceholderCapability(
                    AiCapabilityId.DASHBOARD_INSIGHTS,
                    "Generate dashboard insights from user context.",
                    objectMapper);
        }
    }
}
