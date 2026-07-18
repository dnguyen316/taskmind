package com.taskmind.backend.auth.infrastructure.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

class AuthE2eBypassGuardTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withBean(AuthE2eBypassGuard.class);

    @Test
    void productionProfileRejectsEnabledBypass() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        assertThatThrownBy(() -> new AuthE2eBypassGuard(environment, true, true).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one of the local, test, or e2e profiles");
    }

    @Test
    void stagingProfileRejectsExplicitlyEnabledBypass() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("staging");
        assertThatThrownBy(() -> new AuthE2eBypassGuard(environment, true, true).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exactly one of the local, test, or e2e profiles");
    }

    @Test
    void enabledBypassRequiresSecondaryDangerousSeedOptIn() {
        var environment = new MockEnvironment();
        environment.setActiveProfiles("e2e");
        assertThatThrownBy(() -> new AuthE2eBypassGuard(environment, true, false).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("allow-dangerous-local-seed=true");
    }

    @Test
    void mixedPublicProfileRejectsEnabledBypassAtStartup() {
        contextRunner.withPropertyValues("spring.profiles.active=prod,local", "taskmind.auth.e2e-bypass.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage(
                                    "E2E authentication bypass requires taskmind.auth.e2e-bypass.allow-dangerous-local-seed=true");
                });
    }

    @Test
    void mixedPublicProfileRejectsEnabledBypassEvenWithSecondaryOptIn() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=staging,local",
                        "taskmind.auth.e2e-bypass.enabled=true",
                        "taskmind.auth.e2e-bypass.allow-dangerous-local-seed=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage(
                                    "E2E authentication bypass may only be enabled with exactly one of the local, test, or e2e profiles");
                });
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
    void localProfileKeepsBypassDisabledByDefault() {
        contextRunner.withPropertyValues("spring.profiles.active=local").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getProperty("taskmind.auth.e2e-bypass.enabled", Boolean.class))
                    .isFalse();
            assertThat(context.getEnvironment().getProperty("taskmind.auth.otp.fixed-code")).isEmpty();
        });
    }

    @Test
    void dedicatedE2eProfileEnablesBypassForIsolatedBrowserE2e() {
        contextRunner.withPropertyValues("spring.profiles.active=e2e").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getProperty("taskmind.auth.e2e-bypass.enabled", Boolean.class))
                    .isTrue();
            assertThat(context.getEnvironment()
                            .getProperty("taskmind.auth.e2e-bypass.allow-dangerous-local-seed", Boolean.class))
                    .isTrue();
            assertThat(context.getEnvironment().getProperty("taskmind.auth.otp.fixed-code")).isEqualTo("1");
        });
    }
}
