package com.taskmind.backend.ratelimit;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "taskmind.ratelimit")
public class RateLimitProperties {

    private boolean enabled = true;
    private String keyPrefix = "taskmind:ratelimit";
    @Valid private Bucket anonymous = new Bucket(120, Duration.ofMinutes(1));
    @Valid private Bucket authenticated = new Bucket(600, Duration.ofMinutes(1));
    @Valid private Bucket authFlow = new Bucket(20, Duration.ofMinutes(1));
    @Valid private Bucket aiHeavy = new Bucket(30, Duration.ofMinutes(1));
    private List<String> authFlowPaths = List.of("/v1/auth/");
    private List<String> aiHeavyPaths = List.of("/v1/ai/", "/v1/spec-breakdown/");
    private List<String> publicApiPrefixes = List.of("/v1/", "/api/health");
    private List<String> internalPrefixes = List.of("/internal/");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public Bucket getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(Bucket anonymous) {
        this.anonymous = anonymous;
    }

    public Bucket getAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(Bucket authenticated) {
        this.authenticated = authenticated;
    }

    public Bucket getAuthFlow() {
        return authFlow;
    }

    public void setAuthFlow(Bucket authFlow) {
        this.authFlow = authFlow;
    }

    public Bucket getAiHeavy() {
        return aiHeavy;
    }

    public void setAiHeavy(Bucket aiHeavy) {
        this.aiHeavy = aiHeavy;
    }

    public List<String> getAuthFlowPaths() {
        return authFlowPaths;
    }

    public void setAuthFlowPaths(List<String> authFlowPaths) {
        this.authFlowPaths = authFlowPaths;
    }

    public List<String> getAiHeavyPaths() {
        return aiHeavyPaths;
    }

    public void setAiHeavyPaths(List<String> aiHeavyPaths) {
        this.aiHeavyPaths = aiHeavyPaths;
    }

    public List<String> getPublicApiPrefixes() {
        return publicApiPrefixes;
    }

    public void setPublicApiPrefixes(List<String> publicApiPrefixes) {
        this.publicApiPrefixes = publicApiPrefixes;
    }

    public List<String> getInternalPrefixes() {
        return internalPrefixes;
    }

    public void setInternalPrefixes(List<String> internalPrefixes) {
        this.internalPrefixes = internalPrefixes;
    }

    public record Bucket(@Min(1) long capacity, Duration refillPeriod) {}
}
