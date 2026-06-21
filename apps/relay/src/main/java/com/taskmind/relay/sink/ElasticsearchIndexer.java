package com.taskmind.relay.sink;

import com.taskmind.events.DomainEvent;
import com.taskmind.relay.search.ActivityEventDocument;
import com.taskmind.relay.search.ActivityEventSearchRepository;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchIndexer {
    static final List<String> DEFAULT_RECOMMENDATION_ENTITY_TYPES =
            List.of("task", "project", "attachment", "document", "spec", "spec-document");

    private final Optional<ActivityEventSearchRepository> repository;
    private final Set<String> recommendationEntityTypes;

    public ElasticsearchIndexer(
            Optional<ActivityEventSearchRepository> repository,
            @Value("${taskmind.activity-search.recommendation-entity-types:task,project,attachment,document,spec,spec-document}")
                    List<String> recommendationEntityTypes) {
        this.repository = repository;
        this.recommendationEntityTypes = normalize(recommendationEntityTypes);
    }

    public void index(DomainEvent event) {
        if (!recommendationEntityTypes.contains(normalize(event.entity().type()))) {
            return;
        }
        repository.ifPresent(search -> search.index(ActivityEventDocument.from(event)));
    }

    private Set<String> normalize(List<String> entityTypes) {
        return entityTypes.stream()
                .map(this::normalize)
                .filter(entityType -> !entityType.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalize(String entityType) {
        return entityType == null ? "" : entityType.trim().toLowerCase(Locale.ROOT);
    }
}
