package com.taskmind.backend.ai.application;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiFacadeLocalFallbacks {
    public CaptureResult capture(String text) {
        List<String> lines = text.lines().map(String::trim).filter(line -> !line.isBlank()).limit(10).toList();
        ArrayList<CaptureResult.CapturedTaskDraft> drafts = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            drafts.add(new CaptureResult.CapturedTaskDraft(
                    lines.get(i), "TODO", 3, 30, "AI_CAPTURE", Math.max(0.5d, 0.9d - (i * 0.05d))));
        }
        return new CaptureResult(drafts, drafts.isEmpty() ? "Could you share at least one action-oriented task?" : null);
    }

    public DescribeTaskResult describe(String title, String notes) {
        String suffix = notes == null || notes.isBlank() ? "" : " Notes: " + notes.trim();
        return new DescribeTaskResult("Complete: " + title.trim() + "." + suffix, "Generated locally from the task title and notes.");
    }

    public DescribeTaskAutocompleteResult autocomplete(String text) {
        String base = text == null || text.isBlank() ? "Add acceptance criteria" : text.trim();
        return new DescribeTaskAutocompleteResult(List.of(base + " with clear acceptance criteria.", base + " and identify blockers."));
    }

    public TranslateTaskResult translate(String text, String targetLanguage) {
        return new TranslateTaskResult("[" + targetLanguage + "] " + text.trim(), targetLanguage);
    }
}
