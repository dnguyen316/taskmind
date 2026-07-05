package com.taskmind.backend.task.application;

import com.taskmind.backend.auth.AuthenticatedUser;
import com.taskmind.backend.task.domain.model.SavedTaskView;
import com.taskmind.backend.task.infrastructure.persistence.jpa.SavedTaskViewJpaEntity;
import com.taskmind.backend.task.infrastructure.persistence.jpa.SpringDataSavedTaskViewJpaRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedTaskViewService {
    private final SpringDataSavedTaskViewJpaRepository views;
    public SavedTaskViewService(SpringDataSavedTaskViewJpaRepository views){this.views=views;}
    public List<SavedTaskView> list(AuthenticatedUser user){return views.findByUserIdOrderByCreatedAtAsc(user.userId()).stream().map(SavedTaskViewJpaEntity::toDomain).toList();}
    @Transactional
    public SavedTaskView create(AuthenticatedUser user, String name, String filtersJson){
        Instant now=Instant.now();
        return views.save(SavedTaskViewJpaEntity.fromDomain(new SavedTaskView(UUID.randomUUID(),null,user.userId(),name.trim(),filtersJson,false,now,now))).toDomain();
    }
    @Transactional
    public boolean delete(AuthenticatedUser user, UUID id){
        return views.findByIdAndUserId(id,user.userId()).map(v->{views.delete(v); return true;}).orElse(false);
    }
}
