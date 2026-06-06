package com.taskmind.relay.ingest;

import com.taskmind.events.DomainEvent;
import com.taskmind.events.DomainEventMapper;
import com.taskmind.events.DomainEventValidator;
import com.taskmind.relay.dlq.RelayDeadLetterWriter;
import com.taskmind.relay.observability.RelayPipelineMetrics;
import com.taskmind.relay.projection.DailyMetricsProjector;
import com.taskmind.relay.projection.ProjectProjectionHandler;
import com.taskmind.relay.projection.TaskProjectionHandler;
import com.taskmind.relay.sink.EventStoreWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IngestApplicationService {
    private final DomainEventMapper mapper = new DomainEventMapper();
    private final DomainEventValidator validator = new DomainEventValidator();
    private final EventStoreWriter eventStoreWriter;
    private final TaskProjectionHandler taskProjectionHandler;
    private final ProjectProjectionHandler projectProjectionHandler;
    private final DailyMetricsProjector dailyMetricsProjector;
    private final RelayDeadLetterWriter deadLetterWriter;
    private final RelayPipelineMetrics metrics;

    public IngestApplicationService(EventStoreWriter eventStoreWriter, TaskProjectionHandler taskProjectionHandler, ProjectProjectionHandler projectProjectionHandler, DailyMetricsProjector dailyMetricsProjector, RelayDeadLetterWriter deadLetterWriter, RelayPipelineMetrics metrics) {
        this.eventStoreWriter = eventStoreWriter;
        this.taskProjectionHandler = taskProjectionHandler;
        this.projectProjectionHandler = projectProjectionHandler;
        this.dailyMetricsProjector = dailyMetricsProjector;
        this.deadLetterWriter = deadLetterWriter;
        this.metrics = metrics;
    }

    @Transactional
    public boolean ingest(String rawPayload) {
        DomainEvent event = null;
        try {
            event = mapper.fromJson(rawPayload);
            validator.validate(event);
            if (!eventStoreWriter.writeIfNew(event)) {
                metrics.recordDuplicate();
                return false;
            }
            taskProjectionHandler.project(event);
            projectProjectionHandler.project(event);
            dailyMetricsProjector.project(event);
            metrics.recordIngested();
            return true;
        } catch (RuntimeException ex) {
            deadLetterWriter.write(event, rawPayload, ex);
            metrics.recordDeadLetter();
            return false;
        }
    }
}
