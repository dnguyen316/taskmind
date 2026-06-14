package com.taskmind.ai.capability;

import com.taskmind.ai.contracts.AiCapabilityId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class CapabilityRegistry {
    private final Map<AiCapabilityId, Capability> capabilities;

    public CapabilityRegistry(List<Capability> capabilities) {
        this.capabilities =
                capabilities.stream()
                        .collect(Collectors.toUnmodifiableMap(Capability::id, Function.identity()));
    }

    public Optional<Capability> find(AiCapabilityId id) {
        return Optional.ofNullable(capabilities.get(id));
    }

    public List<CapabilityDescriptor> list() {
        return capabilities.values().stream()
                .sorted(Comparator.comparing(capability -> capability.id().value()))
                .map(
                        capability ->
                                new CapabilityDescriptor(
                                        capability.id().value(),
                                        capability.description(),
                                        capability.inputSchema(),
                                        capability.outputSchema()))
                .toList();
    }
}
