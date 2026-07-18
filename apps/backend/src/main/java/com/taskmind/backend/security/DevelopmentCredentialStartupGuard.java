package com.taskmind.backend.security;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DevelopmentCredentialStartupGuard {
    private static final Set<String> ALLOWED_DEVELOPMENT_PROFILES = Set.of("local", "test", "e2e");
    private static final Map<String, String> DEVELOPMENT_CREDENTIALS = Map.of(
            "taskmind.auth.jwt.secret",
            "development-only-taskmind-jwt-secret-change-me",
            "taskmind.nova.service-token",
            "development-only-nova-service-token",
            "taskmind.relay.client.service-token",
            "local-relay-service-token");

    private final Environment environment;
    private final String jwtSecret;
    private final String novaServiceToken;
    private final String relayServiceToken;

    public DevelopmentCredentialStartupGuard(
            Environment environment,
            @Value("${taskmind.auth.jwt.secret:}") String jwtSecret,
            @Value("${taskmind.nova.service-token:}") String novaServiceToken,
            @Value("${taskmind.relay.client.service-token:}") String relayServiceToken) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.novaServiceToken = novaServiceToken;
        this.relayServiceToken = relayServiceToken;
    }

    @PostConstruct
    void validate() {
        if (usesOnlyAllowedDevelopmentProfiles()) {
            return;
        }
        rejectIfDevelopmentCredential("taskmind.auth.jwt.secret", jwtSecret);
        rejectIfDevelopmentCredential("taskmind.nova.service-token", novaServiceToken);
        rejectIfDevelopmentCredential("taskmind.relay.client.service-token", relayServiceToken);
    }

    private boolean usesOnlyAllowedDevelopmentProfiles() {
        java.util.List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        return activeProfiles.size() == 1 && ALLOWED_DEVELOPMENT_PROFILES.contains(activeProfiles.get(0));
    }

    private void rejectIfDevelopmentCredential(String propertyName, String value) {
        if (DEVELOPMENT_CREDENTIALS.get(propertyName).equals(value)) {
            throw new IllegalStateException(propertyName + " must be changed for non-local/non-test deployments");
        }
    }
}
