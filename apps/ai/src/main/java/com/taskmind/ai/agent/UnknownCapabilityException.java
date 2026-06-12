package com.taskmind.ai.agent;

public class UnknownCapabilityException extends RuntimeException {
    public UnknownCapabilityException(String capabilityId) {
        super("Unknown AI capability: " + capabilityId);
    }
}
