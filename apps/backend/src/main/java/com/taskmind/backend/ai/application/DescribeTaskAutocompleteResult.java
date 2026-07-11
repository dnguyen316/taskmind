package com.taskmind.backend.ai.application;

import java.util.List;

public record DescribeTaskAutocompleteResult(List<String> suggestions, AiResponseSource source, boolean degraded) {}
