package com.taskmind.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RemoteSecretsEnvironmentGuardTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withBean(RemoteSecretsEnvironmentGuard.class);

    @ParameterizedTest
    @ValueSource(strings = {"staging", "prod"})
    void remoteProfileRejectsMissingSecrets(String profile) {
        contextRunner
                .withPropertyValues("spring.profiles.active=" + profile)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasMessageContaining("spring.datasource.url");
                });
    }
}
