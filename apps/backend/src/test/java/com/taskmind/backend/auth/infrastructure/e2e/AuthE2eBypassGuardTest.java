package com.taskmind.backend.auth.infrastructure.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.mock.env.MockEnvironment;

class AuthE2eBypassGuardTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withBean(AuthE2eBypassGuard.class);

    @Test
    void productionProfileRejectsEnabledBypass() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        assertThatThrownBy(() -> new AuthE2eBypassGuard(environment, true).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("may only be enabled with local, test, or e2e profiles");
    }

    @Test
    void stagingProfileRejectsExplicitlyEnabledBypass() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("staging");
        assertThatThrownBy(() -> new AuthE2eBypassGuard(environment, true).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("may only be enabled with local, test, or e2e profiles");
    }

    @Test
    void stagingProfileKeepsBypassDisabledByDefault() {
        contextRunner.withPropertyValues("spring.profiles.active=staging").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getProperty("taskmind.auth.e2e-bypass.enabled", Boolean.class))
                    .isFalse();
            assertThat(context.getEnvironment().getProperty("taskmind.auth.otp.fixed-code")).isEmpty();
        });
    }

    @Test
    void stagingWithE2eProfileEnablesBypassForIsolatedBrowserE2e() {
        contextRunner.withPropertyValues("spring.profiles.active=staging,e2e").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getProperty("taskmind.auth.e2e-bypass.enabled", Boolean.class))
                    .isTrue();
            assertThat(context.getEnvironment().getProperty("taskmind.auth.otp.fixed-code")).isEqualTo("1");
        });
    }
}
