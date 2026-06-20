package com.taskmind.ai.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.taskmind.ai.contracts.activity.ActivitySearchAssistRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActivitySearchAssistServiceTest {
    private final ActivitySearchAssistService service = new ActivitySearchAssistService();

    @Test
    void returnsSafeStructuredQuery() {
        var response =
                service.assist(
                        new ActivitySearchAssistRequest(
                                UUID.randomUUID(),
                                "show completed onboarding activity OR title:*",
                                "onboarding",
                                List.of()));

        assertThat(response.query()).contains("onboarding").contains("DONE");
        assertThat(response.query()).doesNotContain("*").doesNotContain(" OR ");
        assertThat(response.suggestedFilters()).isEmpty();
    }

    @Test
    void rejectsUnsupportedFilters() {
        assertThatThrownBy(
                        () ->
                                service.assist(
                                        new ActivitySearchAssistRequest(
                                                UUID.randomUUID(), "find tasks", null, List.of("status"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported activity search filters");
    }
}
