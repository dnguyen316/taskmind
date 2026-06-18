package com.taskmind.backend.specbreakdown.template;

import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownTemplate;
import com.taskmind.backend.specbreakdown.domain.repository.SpecBreakdownTemplateRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemorySpecBreakdownTemplateRepository implements SpecBreakdownTemplateRepository {
    private final Map<UUID, SpecBreakdownTemplate> templates = new LinkedHashMap<>();

    public SpecBreakdownTemplate save(SpecBreakdownTemplate template) {
        SpecBreakdownTemplate persisted = new SpecBreakdownTemplate(
                template.id(),
                template.version() == null ? 0L : template.version() + 1L,
                template.projectId(),
                template.name(),
                template.description(),
                template.fields(),
                template.createdAt(),
                template.updatedAt());
        templates.put(template.id(), persisted);
        return persisted;
    }

    public Optional<SpecBreakdownTemplate> findById(UUID id) {
        return Optional.ofNullable(templates.get(id));
    }

    public List<SpecBreakdownTemplate> findByProjectId(UUID projectId) {
        return new ArrayList<>(templates.values()).stream()
                .filter(template -> template.projectId().equals(projectId))
                .sorted(Comparator.comparing(SpecBreakdownTemplate::updatedAt).reversed())
                .toList();
    }

    public void delete(SpecBreakdownTemplate template) {
        templates.remove(template.id());
    }
}
