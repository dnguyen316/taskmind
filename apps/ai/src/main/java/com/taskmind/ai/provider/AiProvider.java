package com.taskmind.ai.provider;

import com.taskmind.ai.contracts.AiProviderId;

public interface AiProvider {
    AiProviderId id();

    String modelId();

    ProviderResponse complete(ProviderRequest request);
}
