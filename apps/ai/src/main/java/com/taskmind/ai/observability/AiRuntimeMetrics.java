package com.taskmind.ai.observability;

import com.taskmind.ai.contracts.AiCapabilityId;
import com.taskmind.ai.contracts.AiProviderId;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class AiRuntimeMetrics {
    public static final String PROMPT_TOKENS = "taskmind.ai.tokens.prompt";
    public static final String COMPLETION_TOKENS = "taskmind.ai.tokens.completion";
    public static final String TOTAL_TOKENS = "taskmind.ai.tokens.total";
    public static final String RESPONSE_DURATION = "taskmind.ai.llm.response.duration";
    public static final String RUNS_TOTAL = "taskmind.ai.runs.total";

    private final MeterRegistry meterRegistry;

    public AiRuntimeMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordSuccess(
            AiProviderId provider,
            String model,
            AiCapabilityId capability,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            long latencyMs) {
        Tags tags = tags(provider, model, capability, "success");
        increment(PROMPT_TOKENS, tags, promptTokens);
        increment(COMPLETION_TOKENS, tags, completionTokens);
        increment(TOTAL_TOKENS, tags, totalTokens);
        recordLatency(tags, latencyMs);
        incrementRun(tags);
    }

    public void recordFailure(
            AiProviderId provider, String model, AiCapabilityId capability, long latencyMs) {
        Tags tags = tags(provider, model, capability, "failure");
        recordLatency(tags, latencyMs);
        incrementRun(tags);
    }

    private void increment(String metricName, Tags tags, int amount) {
        if (amount > 0) {
            meterRegistry.counter(metricName, tags).increment(amount);
        }
    }

    private void recordLatency(Tags tags, long latencyMs) {
        meterRegistry
                .timer(RESPONSE_DURATION, tags)
                .record(Math.max(0L, latencyMs), TimeUnit.MILLISECONDS);
    }

    private void incrementRun(Tags tags) {
        meterRegistry.counter(RUNS_TOTAL, tags).increment();
    }

    private Tags tags(AiProviderId provider, String model, AiCapabilityId capability, String status) {
        return Tags.of(
                "provider",
                provider.value(),
                "model",
                model,
                "capability",
                capability.value(),
                "status",
                status);
    }
}
