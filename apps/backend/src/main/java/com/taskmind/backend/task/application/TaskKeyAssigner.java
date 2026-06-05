package com.taskmind.backend.task.application;

import com.taskmind.backend.project.domain.repository.ProjectRepository;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskKeyAssigner {
    private final JdbcTemplate jdbc; private final ProjectRepository projects;
    public TaskKeyAssigner(JdbcTemplate jdbc, ProjectRepository projects){this.jdbc=jdbc;this.projects=projects;}
    @Transactional
    public synchronized String assign(UUID projectId) {
        if (projectId == null) return null;
        int updated=jdbc.update("update project_task_sequences set next_value=next_value+1 where project_id=?",projectId);
        if(updated==0){try{jdbc.update("insert into project_task_sequences(project_id,next_value) values (?,2)",projectId);}catch(DataIntegrityViolationException ignored){jdbc.update("update project_task_sequences set next_value=next_value+1 where project_id=?",projectId);}}
        Long value=jdbc.queryForObject("select next_value-1 from project_task_sequences where project_id=?",Long.class,projectId);
        String key=projects.findById(projectId).orElseThrow(()->new IllegalArgumentException("Project not found")).key();
        return key+"-"+value;
    }
}
