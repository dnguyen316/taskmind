package com.taskmind.backend.task.application;
import com.taskmind.backend.task.domain.model.*; import java.math.BigDecimal; import java.time.OffsetDateTime; import java.util.UUID;
public record CreateTaskCommand(UUID userId,UUID projectId,UUID assigneeId,UUID parentTaskId,TaskLevel taskLevel,String taskType,Integer storyPoints,String releaseVersion,String title,String description,TaskStatus status,int priority,OffsetDateTime dueAt,Integer durationMinutes,EnergyLevel energyLevel,TaskSource source,BigDecimal confidence){
 public CreateTaskCommand(UUID userId,UUID projectId,String title,String description,TaskStatus status,int priority,OffsetDateTime dueAt,Integer durationMinutes,EnergyLevel energyLevel,TaskSource source,BigDecimal confidence){this(userId,projectId,null,null,TaskLevel.TASK,"TASK",null,null,title,description,status,priority,dueAt,durationMinutes,energyLevel,source,confidence);}
}
