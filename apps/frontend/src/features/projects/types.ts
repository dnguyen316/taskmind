export type ProjectMembershipRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER'

export interface Project {
  id: string
  name: string
  key: string
  description: string | null
  ownerUserId: string
  createdAt: string
  updatedAt: string
  archivedAt: string | null
  version?: number
}

export interface ProjectMembership {
  projectId: string
  userId: string
  role: ProjectMembershipRole
  createdAt: string
}

export interface CreateProjectPayload {
  name: string
  key: string
  description: string | null
  ownerUserId: string
}

export interface UpdateProjectPayload {
  name?: string
  key?: string
  description?: string | null
}

export interface AddProjectMemberPayload {
  userId: string
  role: ProjectMembershipRole
}
