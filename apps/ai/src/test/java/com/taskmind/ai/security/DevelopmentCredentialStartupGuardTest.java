package com.taskmind.ai.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DevelopmentCredentialStartupGuardTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withBean(DevelopmentCredentialStartupGuard.class);

    @Test
    void defaultProfileRejectsDevelopmentCredentialDefaults() {
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("taskmind.ai.service-token must be changed for non-local/non-test deployments");
        });
    }

    @Test
    void localProfileAllowsDevelopmentCredentialDefaults() {
        contextRunner.withPropertyValues("spring.profiles.active=local").run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void testProfileAllowsDevelopmentCredentialDefaults() {
        contextRunner.withPropertyValues("spring.profiles.active=test").run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void e2eProfileAllowsDevelopmentCredentialDefaults() {
        contextRunner.withPropertyValues("spring.profiles.active=e2e").run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void prodLikeProfileRejectsDevelopmentCoreToken() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "taskmind.ai.service-token=prod-ai-token",
                        "taskmind.core.service-token=local-core-token",
                        "taskmind.relay.service-token=prod-relay-token")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage(
                                    "taskmind.core.service-token must be changed for non-local/non-test deployments");
                });
    }

    @Test
    void prodLikeProfileRejectsDevelopmentRelayToken() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "taskmind.ai.service-token=prod-ai-token",
                        "taskmind.core.service-token=prod-core-token",
                        "taskmind.relay.service-token=local-relay-token")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage(
                                    "taskmind.relay.service-token must be changed for non-local/non-test deployments");
                });
    }

    @Test
    void prodLikeProfileAcceptsChangedCredentials() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "taskmind.ai.service-token=prod-ai-token",
                        "taskmind.core.service-token=prod-core-token",
                        "taskmind.relay.service-token=prod-relay-token")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
