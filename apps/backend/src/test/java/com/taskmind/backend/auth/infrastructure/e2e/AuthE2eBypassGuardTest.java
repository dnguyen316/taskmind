package com.taskmind.backend.auth.infrastructure.e2e;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class AuthE2eBypassGuardTest {
    @Test void productionProfileRejectsEnabledBypass() {
        var environment=new MockEnvironment(); environment.setActiveProfiles("prod");
        assertThatThrownBy(() -> new AuthE2eBypassGuard(environment,true).validate())
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("must not be enabled in prod");
    }
}
