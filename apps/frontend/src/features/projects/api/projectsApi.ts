import { apiClient } from '../../../lib/apiClient'

export interface ProjectRecord {
  id: string
  archived?: boolean | null
  [key: string]: unknown
}

export interface MemberRecord {
  id: string
  [key: string]: unknown
}

export async function fetchProjects({ includeArchived = false }: { includeArchived?: boolean } = {}) {
  const response = await apiClient.get<ProjectRecord[]>('/v1/projects', {
    params: { includeArchived },
  })

  return response.data
}

export async function fetchProject(projectId: string) {
  const response = await apiClient.get<ProjectRecord>(`/v1/projects/${projectId}`)
  return response.data
}

export async function submitProject(payload: Record<string, unknown>) {
  const response = await apiClient.post<ProjectRecord>('/v1/projects', payload)
  return response.data
}

export async function saveProject(projectId: string, payload: Record<string, unknown>) {
  const response = await apiClient.patch<ProjectRecord>(`/v1/projects/${projectId}`, payload)
  return response.data
}

export async function archiveProjectById(projectId: string) {
  await apiClient.patch(`/v1/projects/${projectId}/archive`)
}

export async function fetchMembers(projectId: string) {
  const response = await apiClient.get<MemberRecord[]>(`/v1/projects/${projectId}/members`)
  return response.data
}

export async function addMember(projectId: string, payload: Record<string, unknown>) {
  const response = await apiClient.post<MemberRecord>(`/v1/projects/${projectId}/members`, payload)
  return response.data
}

export async function removeMember(projectId: string, memberId: string) {
  await apiClient.delete(`/v1/projects/${projectId}/members/${memberId}`)
}
