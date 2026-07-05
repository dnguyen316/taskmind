package com.taskmind.backend.task.infrastructure.persistence.jpa;

import com.taskmind.backend.task.domain.model.SavedTaskView;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "task_saved_views")
public class SavedTaskViewJpaEntity {
    @Id private UUID id;
    @Version private Long version;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false) private String name;
    @Column(name = "filters_json", nullable = false, columnDefinition = "TEXT") private String filtersJson;
    @Column(name = "built_in", nullable = false) private boolean builtIn;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected SavedTaskViewJpaEntity() {}
    private SavedTaskViewJpaEntity(SavedTaskView v){id=v.id();version=v.version();userId=v.userId();name=v.name();filtersJson=v.filtersJson();builtIn=v.builtIn();createdAt=v.createdAt();updatedAt=v.updatedAt();}
    public static SavedTaskViewJpaEntity fromDomain(SavedTaskView v){return new SavedTaskViewJpaEntity(v);}
    public SavedTaskView toDomain(){return new SavedTaskView(id,version,userId,name,filtersJson,builtIn,createdAt,updatedAt);}
}
