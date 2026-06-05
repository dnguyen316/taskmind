package com.taskmind.backend.task.application;
import com.taskmind.backend.task.domain.model.*; import java.time.OffsetDateTime; import java.util.UUID;
public record UpdateTaskCommand(Long version,UUID projectId,UUID assigneeId,UUID parentTaskId,TaskLevel taskLevel,TaskType taskType,Integer storyPoints,String releaseVersion,String title,String description,TaskStatus status,Integer priority,OffsetDateTime dueAt,Integer durationMinutes,EnergyLevel energyLevel){
 public UpdateTaskCommand(UUID projectId,String title,String description,TaskStatus status,Integer priority,OffsetDateTime dueAt,Integer durationMinutes,EnergyLevel energyLevel){this(null,projectId,null,null,null,null,null,null,title,description,status,priority,dueAt,durationMinutes,energyLevel);}
}
