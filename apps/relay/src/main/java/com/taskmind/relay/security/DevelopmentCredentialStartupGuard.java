package com.taskmind.relay.security;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DevelopmentCredentialStartupGuard {
    private static final Set<String> ALLOWED_DEVELOPMENT_PROFILES = Set.of("local", "test", "e2e");
    private static final String DEVELOPMENT_RELAY_SERVICE_TOKEN = "local-relay-service-token";

    private final Environment environment;
    private final String relayServiceToken;

    public DevelopmentCredentialStartupGuard(
            Environment environment, @Value("${taskmind.relay.service-token:}") String relayServiceToken) {
        this.environment = environment;
        this.relayServiceToken = relayServiceToken;
    }

    @PostConstruct
    void validate() {
        if (usesOnlyAllowedDevelopmentProfiles()) {
            return;
        }
        if (DEVELOPMENT_RELAY_SERVICE_TOKEN.equals(relayServiceToken)) {
            throw new IllegalStateException(
                    "taskmind.relay.service-token must be changed for non-local/non-test deployments");
        }
    }

    private boolean usesOnlyAllowedDevelopmentProfiles() {
        java.util.List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        return activeProfiles.size() == 1 && ALLOWED_DEVELOPMENT_PROFILES.contains(activeProfiles.get(0));
    }
}
