package com.taskmind.ai.capability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.taskmind.ai.agent.AiAgentRuntime;
import com.taskmind.ai.audit.AiRunAuditRepository;
import com.taskmind.ai.audit.AiRunRecord;
import com.taskmind.ai.contracts.AiProviderId;
import com.taskmind.ai.contracts.AiRunStatus;
import com.taskmind.ai.contracts.capability.CapabilityRequest;
import com.taskmind.ai.provider.AiProvider;
import com.taskmind.ai.provider.MockAiProvider;
import com.taskmind.ai.provider.ProviderRequest;
import com.taskmind.ai.provider.ProviderResponse;
import com.taskmind.ai.provider.ProviderRouter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TaskResolutionAgentCapabilityTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TaskResolutionAgentCapability capability = new TaskResolutionAgentCapability(objectMapper);

    @Test
    void acceptsValidWorkflowAndAddsPromptVersion() {
        JsonNode providerInput = capability.buildProviderInput(validInput());

        assertThat(providerInput.get("promptVersion").asText()).isEqualTo(TaskResolutionAgentCapability.PROMPT_VERSION);
    }

    @Test
    void rejectsInvalidTemplate() {
        ObjectNode input = validInput();
        input.withObject("workflowTemplate").put("id", "unknown-template");

        assertThatThrownBy(() -> capability.buildProviderInput(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported task-resolution workflow template");
    }

    @Test
    void rejectsUnsupportedTool() {
        ObjectNode input = validInput();
        input.putArray("allowedTools").add("browser.github.write");

        assertThatThrownBy(() -> capability.buildProviderInput(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported task-resolution tool");
    }

    @Test
    void mockProviderReturnsDeterministicCoreRoutedToolCalls() {
        MockAiProvider provider = new MockAiProvider("test-model", objectMapper);
        JsonNode providerInput = capability.buildProviderInput(validInput());
        ProviderRequest request = new ProviderRequest(capability.id(), providerInput, TaskResolutionAgentCapability.PROMPT_VERSION, List.of(), "corr-1");

        JsonNode first = provider.complete(request).output();
        JsonNode second = provider.complete(request).output();

        assertThat(first).isEqualTo(second);
        assertThat(first.path("taskId").asText()).isEqualTo("task-123");
        assertThat(first.path("toolCalls").path(0).path("coreInternalEndpoint").asText()).startsWith("/internal/v1/");
    }

    @Test
    void providerFailureReturnsFailedCapabilityResponse() {
        AiProvider failingProvider = new AiProvider() {
            @Override
            public AiProviderId id() {
                return new AiProviderId("failing");
            }

            @Override
            public String modelId() {
                return "failing-model";
            }

            @Override
            public ProviderResponse complete(ProviderRequest request) {
                throw new IllegalStateException("provider down");
            }
        };
        RecordingAuditRepository auditRepository = new RecordingAuditRepository();
        AiAgentRuntime runtime = new AiAgentRuntime(
                new CapabilityRegistry(List.of(capability)),
                new ProviderRouter("failing", List.of(failingProvider)),
                auditRepository,
                objectMapper);

        var response = runtime.execute(new CapabilityRequest(capability.id(), UUID.randomUUID(), "workspace", validInput(), "corr-2", "idem"));

        assertThat(response.status()).isEqualTo(AiRunStatus.FAILED);
        assertThat(response.error().code()).isEqualTo("PROVIDER_ERROR");
        assertThat(auditRepository.started.promptVersion()).isEqualTo(TaskResolutionAgentCapability.PROMPT_VERSION);
        assertThat(auditRepository.started.validationOutcome()).isEqualTo("VALID");
        assertThat(auditRepository.failed).isTrue();
    }

    private ObjectNode validInput() {
        ObjectNode input = objectMapper.createObjectNode();
        input.putObject("task")
                .put("id", "task-123")
                .put("title", "Resolve login bug")
                .put("description", "Investigate and fix failed OAuth callback")
                .put("status", "IN_PROGRESS");
        input.put("projectId", "project-123");
        input.putObject("githubRepository")
                .put("owner", "taskmind")
                .put("name", "taskmind")
                .put("defaultBranch", "main")
                .put("installationId", "42");
        input.putObject("workflowTemplate").put("id", "task-resolution-default").put("version", "1");
        input.putArray("allowedTools").add("core.task.comment");
        input.put("approvalPolicy", "propose-only");
        return input;
    }

    private static class RecordingAuditRepository extends AiRunAuditRepository {
        private AiRunRecord started;
        private boolean failed;

        RecordingAuditRepository() {
            super(null, new ObjectMapper());
        }

        @Override
        public UUID start(AiRunRecord record) {
            this.started = record;
            return UUID.randomUUID();
        }

        @Override
        public void fail(UUID runId, String errorCode, String errorMessage, long latencyMs) {
            this.failed = true;
        }
    }
}
