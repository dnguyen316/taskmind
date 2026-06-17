package com.taskmind.backend.integration.infrastructure.wiki;

import org.springframework.stereotype.Component;

@Component
public class WikiClient {
    public PublishedPage publish(String spaceKey, String title) {
        String id = "wiki-" + Math.abs(title.hashCode());
        return new PublishedPage(id, spaceKey + ":" + title, "https://wiki.example.test/pages/" + id);
    }
    public record PublishedPage(String id, String key, String url) {}
}
