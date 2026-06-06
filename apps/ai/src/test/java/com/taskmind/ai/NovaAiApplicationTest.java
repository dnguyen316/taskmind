package com.taskmind.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class NovaAiApplicationTest {
    @Autowired Environment environment;

    @Test
    void startsWithDeterministicTestProviderConfiguration() {
        assertThat(environment.getProperty("taskmind.ai.provider.default")).isEqualTo("mock");
        assertThat(environment.getProperty("taskmind.ai.provider.openai.api-key")).isEmpty();
        assertThat(environment.getProperty("taskmind.ai.provider.anthropic.api-key")).isEmpty();
    }
}
