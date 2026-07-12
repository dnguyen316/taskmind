package com.taskmind.backend.analytics.application;

import java.util.List;

public class InvalidReportsRangeException extends RuntimeException {
    private final String value;
    private final List<String> allowedValues;

    public InvalidReportsRangeException(String value, List<String> allowedValues) {
        super("Unsupported reports range: " + value);
        this.value = value;
        this.allowedValues = List.copyOf(allowedValues);
    }

    public String value() {
        return value;
    }

    public List<String> allowedValues() {
        return allowedValues;
    }
}
