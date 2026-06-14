package com.taskmind.backend.ai.application;

import java.util.List;

public record CaptureResult(List<CapturedTaskDraft> drafts, String clarificationQuestion) {
    public record CapturedTaskDraft(
            String title,
            String status,
            int priority,
            int durationMinutes,
            String source,
            double confidence) {}
}
