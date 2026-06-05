package com.taskmind.backend.auth.infrastructure.e2e;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AuthE2eBypassGuard {
    private final Environment environment; private final boolean enabled;
    public AuthE2eBypassGuard(Environment environment, @Value("${taskmind.auth.e2e-bypass.enabled:false}") boolean enabled) { this.environment=environment; this.enabled=enabled; }
    @PostConstruct
    void validate() {
        if (enabled && Arrays.asList(environment.getActiveProfiles()).contains("prod")) throw new IllegalStateException("E2E authentication bypass must not be enabled in prod");
    }
}
