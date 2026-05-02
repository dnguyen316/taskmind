import { apiClient } from '../../../lib/apiClient'
import type { Project } from '../types'

export async function listProjects({ userId }: { userId: string }) {
  const response = await apiClient.get<Project[]>('/v1/projects', {
    params: { userId },
  })

  return Array.isArray(response.data) ? response.data : []
}
