package com.taskmind.relay.sink;

import com.taskmind.events.DomainEvent;
import com.taskmind.relay.search.ActivityEventDocument;
import com.taskmind.relay.search.ActivityEventSearchRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchIndexer {
    private final Optional<ActivityEventSearchRepository> repository;

    public ElasticsearchIndexer(Optional<ActivityEventSearchRepository> repository) {
        this.repository = repository;
    }

    public void index(DomainEvent event) {
        if (!"task".equals(event.entity().type())) {
            return;
        }
        repository.ifPresent(search -> search.index(ActivityEventDocument.from(event)));
    }
}
