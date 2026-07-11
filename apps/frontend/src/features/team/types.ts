import type { Project, ProjectMembership, ProjectMembershipRole } from '../projects/types'

export type GlobalRole = 'ADMIN' | 'MANAGER' | 'MEMBER' | string

export interface TeamMember {
  userId: string
  displayName: string
  email: string
  openTasks: number
  globalRole?: GlobalRole | null
}

export interface TeamDirectory {
  members: TeamMember[]
  totalMembers: number
  totalOpenTasks: number
}

export interface TeamMemberProjectAllocation extends ProjectMembership {
  projectName: string
  projectKey: string
  archivedAt: string | null
}

export interface AssignTeamMemberProjectPayload {
  role: ProjectMembershipRole
}

export interface ChangeTeamMemberProjectRolePayload {
  role: ProjectMembershipRole
}

export interface ChangeTeamMemberGlobalRolePayload {
  role: GlobalRole
}

export interface GlobalRoleResponse {
  userId: string
  role: GlobalRole
}

export interface TeamProjectAllocationOption extends Project {
  membership?: ProjectMembership
}
