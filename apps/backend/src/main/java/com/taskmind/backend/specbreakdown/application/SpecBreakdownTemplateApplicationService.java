package com.taskmind.backend.specbreakdown.application;

import com.taskmind.backend.specbreakdown.domain.model.SpecBreakdownTemplate;
import com.taskmind.backend.specbreakdown.domain.repository.SpecBreakdownTemplateRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SpecBreakdownTemplateApplicationService {
    private static final String DEFAULT_FIELDS = "{}";

    private final SpecBreakdownTemplateRepository templates;

    public SpecBreakdownTemplateApplicationService(SpecBreakdownTemplateRepository templates) {
        this.templates = templates;
    }

    @Transactional
    public SpecBreakdownTemplate create(UUID projectId, TemplateCommand command) {
        Instant now = Instant.now();
        return templates.save(
                new SpecBreakdownTemplate(
                        UUID.randomUUID(),
                        null,
                        projectId,
                        command.name().trim(),
                        command.description(),
                        normalizeFields(command.fields(), DEFAULT_FIELDS),
                        now,
                        now));
    }

    public List<SpecBreakdownTemplate> listByProject(UUID projectId) {
        return templates.findByProjectId(projectId);
    }

    @Transactional
    public Optional<SpecBreakdownTemplate> update(UUID id, TemplateCommand command) {
        return templates.findById(id)
                .map(existing -> templates.save(new SpecBreakdownTemplate(
                        existing.id(),
                        existing.version(),
                        existing.projectId(),
                        command.name().trim(),
                        command.description(),
                        normalizeFields(command.fields(), existing.fields()),
                        existing.createdAt(),
                        Instant.now())));
    }

    @Transactional
    public boolean delete(UUID id) {
        return templates.findById(id)
                .map(template -> {
                    templates.delete(template);
                    return true;
                })
                .orElse(false);
    }

    private String normalizeFields(String fields, String fallback) {
        return fields == null || fields.isBlank() ? fallback : fields;
    }

    public record TemplateCommand(String name, String description, String fields) {}
}
