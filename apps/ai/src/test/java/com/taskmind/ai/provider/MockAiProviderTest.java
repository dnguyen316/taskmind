package com.taskmind.ai.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.List;
import org.junit.jupiter.api.Test;

class MockAiProviderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsDeterministicOutputForSameInput() {
        MockAiProvider provider = new MockAiProvider("test-model", objectMapper);
        ProviderRequest request =
                new ProviderRequest(
                        AiCapabilityId.CAPTURE,
                        objectMapper.createObjectNode().put("text", "remember the launch plan"),
                        "test.v1",
                        List.of("user:remember the launch plan"),
                        "corr-1");

        ProviderResponse first = provider.complete(request);
        ProviderResponse second = provider.complete(request);

        assertThat(first).isEqualTo(second);
        assertThat(first.output().get("provider").asText()).isEqualTo("mock");
        assertThat(first.output().get("fingerprint").asText()).hasSize(12);
    }
}
