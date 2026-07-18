package com.taskmind.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RefreshCookieSecurityPropertiesGuardTest {
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withBean(RefreshCookieSecurityPropertiesGuard.class);

    @Test
    void prodProfileFailsStartupWhenRefreshCookiesAreNotSecure() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "taskmind.auth.cookies.secure=false")
                .run(context ->
                        assertThat(context.getStartupFailure())
                                .hasRootCauseInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("taskmind.auth.cookies.secure must be true"));
    }

    @Test
    void testProfileAllowsInsecureRefreshCookies() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=test",
                        "taskmind.auth.cookies.secure=false")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
