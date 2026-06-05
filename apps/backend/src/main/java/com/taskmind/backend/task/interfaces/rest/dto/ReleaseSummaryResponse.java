package com.taskmind.backend.task.interfaces.rest.dto; import java.util.List; import java.util.UUID; public record ReleaseSummaryResponse(UUID projectId,List<ProjectReleaseResponse> releases){}
