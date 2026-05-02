import { apiClient } from '../../../lib/apiClient'
import type {
  AddProjectMemberPayload,
  CreateProjectPayload,
  Project,
  ProjectMembership,
  UpdateProjectPayload,
} from '../types'

export async function listProjects({ includeArchived = false }: { includeArchived?: boolean } = {}) {
  const response = await apiClient.get<Project[]>('/v1/projects', {
    params: {
      includeArchived,
    },
  })

  return response.data
}

export async function createProject(payload: CreateProjectPayload) {
  const response = await apiClient.post<Project>('/v1/projects', payload)
  return response.data
}

export async function getProjectById(id: string) {
  const response = await apiClient.get<Project>(`/v1/projects/${id}`)
  return response.data
}

export async function updateProject(id: string, payload: UpdateProjectPayload) {
  const response = await apiClient.patch<Project>(`/v1/projects/${id}`, payload)
  return response.data
}

export async function archiveProject(id: string) {
  const response = await apiClient.patch<Project>(`/v1/projects/${id}/archive`)
  return response.data
}

export async function listProjectMembers(projectId: string) {
  const response = await apiClient.get<ProjectMembership[]>(`/v1/projects/${projectId}/members`)
  return response.data
}

export async function addProjectMember(projectId: string, payload: AddProjectMemberPayload) {
  const response = await apiClient.post<ProjectMembership>(`/v1/projects/${projectId}/members`, payload)
  return response.data
}

export async function removeProjectMember(projectId: string, userId: string) {
  await apiClient.delete(`/v1/projects/${projectId}/members/${userId}`)
}
