package com.taskmind.ai.provider;

import com.taskmind.ai.contracts.AiProviderId;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProviderRouter {
    private final AiProviderId defaultProviderId;
    private final Map<AiProviderId, AiProvider> providers;

    public ProviderRouter(
            @Value("${taskmind.ai.provider.default:mock}") String defaultProviderId,
            java.util.List<AiProvider> providers) {
        this.defaultProviderId = new AiProviderId(defaultProviderId);
        this.providers =
                providers.stream()
                        .collect(Collectors.toUnmodifiableMap(AiProvider::id, Function.identity()));
    }

    public AiProvider defaultProvider() {
        AiProvider provider = providers.get(defaultProviderId);
        if (provider == null) {
            throw new IllegalStateException(
                    "Configured AI provider is not registered: " + defaultProviderId.value());
        }
        return provider;
    }
}
