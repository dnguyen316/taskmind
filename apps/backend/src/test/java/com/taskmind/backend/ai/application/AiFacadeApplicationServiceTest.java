package com.taskmind.backend.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiRunStatus;
import com.taskmind.ai.contracts.activity.ActivitySearchAssistRequest;
import com.taskmind.ai.contracts.activity.ActivitySearchAssistResponse;
import com.taskmind.ai.contracts.audit.AiRunSummary;
import com.taskmind.ai.contracts.capability.CapabilitiesResponse;
import com.taskmind.ai.contracts.capability.CapabilityRequest;
import com.taskmind.ai.contracts.capability.CapabilityResponse;
import com.taskmind.ai.contracts.chat.ChatRequest;
import com.taskmind.ai.contracts.chat.ChatResponse;
import com.taskmind.backend.ai.NovaClient;
import com.taskmind.backend.ai.NovaClientException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiFacadeApplicationServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final AiFacadeLocalFallbacks fallbacks = new AiFacadeLocalFallbacks();
    private final AiDomainEventPublisher events = (userId, eventType, payload) -> {};
    private final UUID userId = UUID.randomUUID();

    @Test
    void marksNovaSuccessAsAuthoritative() {
        AiFacadeApplicationService service = serviceReturning(Map.of(
                "drafts", List.of(Map.of(
                        "title", "Call customer",
                        "status", "TODO",
                        "priority", 2,
                        "durationMinutes", 25,
                        "source", "AI_CAPTURE",
                        "confidence", 0.91)),
                "clarificationQuestion", ""));

        CaptureResult result = service.capture(userId, "Call customer");

        assertThat(result.source()).isEqualTo(AiResponseSource.NOVA);
        assertThat(result.degraded()).isFalse();
        assertThat(result.drafts()).extracting(CaptureResult.CapturedTaskDraft::title).containsExactly("Call customer");
    }

    @Test
    void marksNovaUnavailableFallbackAsDegraded() {
        AiFacadeApplicationService service = serviceWithNova(new ThrowingNovaClient());

        CaptureResult result = service.capture(userId, "Draft release notes");

        assertThat(result.source()).isEqualTo(AiResponseSource.LOCAL_FALLBACK);
        assertThat(result.degraded()).isTrue();
        assertThat(result.drafts()).extracting(CaptureResult.CapturedTaskDraft::title).containsExactly("Draft release notes");
    }

    @Test
    void marksInvalidNovaShapeFallbackAsDegraded() {
        AiFacadeApplicationService service = serviceReturning(Map.of("unexpected", List.of("shape")));

        WeeklyReviewResult result = service.weeklyReview(userId);

        assertThat(result.source()).isEqualTo(AiResponseSource.LOCAL_FALLBACK);
        assertThat(result.degraded()).isTrue();
        assertThat(result.recommendations()).isNotEmpty();
    }

    @Test
    void fallbackMetadataIsAppliedAcrossAffectedFacadeResponses() {
        AiFacadeApplicationService service = serviceWithNova(new ThrowingNovaClient());

        assertFallback(service.goalBreakdown(userId, UUID.randomUUID(), null, null).source(), service.goalBreakdown(userId, UUID.randomUUID(), null, null).degraded());
        assertFallback(service.weeklyReview(userId).source(), service.weeklyReview(userId).degraded());
        assertFallback(service.projectBrief(userId, UUID.randomUUID(), "Project", null).source(), service.projectBrief(userId, UUID.randomUUID(), "Project", null).degraded());
        assertFallback(service.durationEstimate(userId, "Task", null).source(), service.durationEstimate(userId, "Task", null).degraded());
        assertFallback(service.rationalePhrase(userId, "Task", null).source(), service.rationalePhrase(userId, "Task", null).degraded());
        assertFallback(service.dashboardInsights(userId).source(), service.dashboardInsights(userId).degraded());
        assertFallback(service.describe(userId, "Task", null).source(), service.describe(userId, "Task", null).degraded());
        assertFallback(service.autocomplete(userId, "Task").source(), service.autocomplete(userId, "Task").degraded());
        assertFallback(service.translate(userId, "Task", "French").source(), service.translate(userId, "Task", "French").degraded());
    }

    private void assertFallback(AiResponseSource source, boolean degraded) {
        assertThat(source).isEqualTo(AiResponseSource.LOCAL_FALLBACK);
        assertThat(degraded).isTrue();
    }

    private AiFacadeApplicationService serviceReturning(Object output) {
        return serviceWithNova(new StubNovaClient(new CapabilityResponse(
                UUID.randomUUID(), AiRunStatus.SUCCEEDED, objectMapper.valueToTree(output), List.of(), null)));
    }

    private AiFacadeApplicationService serviceWithNova(NovaClient novaClient) {
        return new AiFacadeApplicationService(novaClient, fallbacks, events, objectMapper);
    }

    private static class StubNovaClient implements NovaClient {
        private final CapabilityResponse response;

        StubNovaClient(CapabilityResponse response) {
            this.response = response;
        }

        @Override
        public CapabilityResponse executeCapability(String capabilityId, CapabilityRequest request) {
            return response;
        }

        @Override public ChatResponse chat(ChatRequest request) { throw new UnsupportedOperationException(); }
        @Override public void chatStream(ChatRequest request, OutputStream outputStream) { throw new UnsupportedOperationException(); }
        @Override public ActivitySearchAssistResponse assistActivitySearch(ActivitySearchAssistRequest request) { throw new UnsupportedOperationException(); }
        @Override public CapabilitiesResponse capabilities() { throw new UnsupportedOperationException(); }
        @Override public AiRunSummary run(UUID runId) { throw new UnsupportedOperationException(); }
    }

    private static class ThrowingNovaClient extends StubNovaClient {
        ThrowingNovaClient() { super(null); }

        @Override
        public CapabilityResponse executeCapability(String capabilityId, CapabilityRequest request) {
            throw new NovaClientException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "NOVA_UNAVAILABLE", "Nova unavailable");
        }
    }
}
