package com.taskmind.relay.config.logging;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

public final class RequestCorrelation {
    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private RequestCorrelation() {}

    public static String currentId() {
        return MDC.get(MDC_KEY);
    }

    public static String currentOrCreate() {
        String current = currentId();
        if (StringUtils.hasText(current)) {
            return current;
        }
        String generated = generate();
        MDC.put(MDC_KEY, generated);
        return generated;
    }

    public static String normalizeOrGenerate(String candidate) {
        if (StringUtils.hasText(candidate)) {
            return candidate.trim();
        }
        return generate();
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
