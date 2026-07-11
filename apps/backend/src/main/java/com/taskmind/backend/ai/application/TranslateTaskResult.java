package com.taskmind.backend.ai.application;

public record TranslateTaskResult(String translatedText, String targetLanguage, AiResponseSource source, boolean degraded) {}
