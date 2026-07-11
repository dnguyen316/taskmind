import { computed, reactive, ref } from 'vue'
import { defineStore } from 'pinia'
import * as projectsApi from '../features/projects/api/projectsApi'
import type { Project, ProjectMembership, ProjectMembershipRole } from '../features/projects/types'
import * as teamApi from '../features/team/api/teamApi'
import type { GlobalRole, TeamDirectory, TeamMemberProjectAllocation } from '../features/team/types'

export const PROJECT_ROLE_OPTIONS: ProjectMembershipRole[] = ['OWNER', 'ADMIN', 'MEMBER', 'VIEWER']
export const GLOBAL_ROLE_OPTIONS: GlobalRole[] = ['ADMIN', 'MANAGER', 'MEMBER']

export const useTeamStore = defineStore('team', () => {
  const directory = ref<TeamDirectory | null>(null)
  const projects = ref<Project[]>([])
  const membershipsByProjectId = reactive<Record<string, ProjectMembership[]>>({})
  const loading = reactive({ directory: false, allocations: false })
  const savingProjectRoleByKey = reactive<Record<string, boolean>>({})
  const savingGlobalRoleByUserId = reactive<Record<string, boolean>>({})
  const loaded = reactive({ directory: false, allocations: false })
  const messages = reactive({ error: '', success: '' })

  const members = computed(() => directory.value?.members ?? [])
  const memberCount = computed(() => directory.value?.totalMembers ?? members.value.length)
  const totalOpenTasks = computed(() => directory.value?.totalOpenTasks ?? 0)
  const activeProjects = computed(() => projects.value.filter((project) => !project.archivedAt))

  const allocationsByUserId = computed<Record<string, TeamMemberProjectAllocation[]>>(() => {
    const next: Record<string, TeamMemberProjectAllocation[]> = {}

    for (const project of projects.value) {
      for (const membership of membershipsByProjectId[project.id] ?? []) {
        next[membership.userId] = [
          ...(next[membership.userId] ?? []),
          {
            ...membership,
            projectName: project.name,
            projectKey: project.key,
            archivedAt: project.archivedAt,
          },
        ]
      }
    }

    return next
  })

  async function fetchDirectory({ force = false }: { force?: boolean } = {}) {
    if (!force && loaded.directory && directory.value) {
      return directory.value
    }

    loading.directory = true
    messages.error = ''

    try {
      directory.value = await teamApi.getTeamDirectory()
      loaded.directory = true
      return directory.value
    } catch (error: unknown) {
      messages.error = error instanceof Error ? error.message : 'Failed to load team directory.'
      throw error
    } finally {
      loading.directory = false
    }
  }

  async function fetchAllocations({ force = false }: { force?: boolean } = {}) {
    if (!force && loaded.allocations) {
      return allocationsByUserId.value
    }

    loading.allocations = true
    messages.error = ''

    try {
      projects.value = await projectsApi.listProjects({ includeArchived: false })
      const membershipEntries = await Promise.all(
        projects.value.map(
          async (project) =>
            [project.id, await projectsApi.listProjectMembers(project.id)] as const,
        ),
      )

      for (const [projectId, memberships] of membershipEntries) {
        membershipsByProjectId[projectId] = memberships
      }

      loaded.allocations = true
      return allocationsByUserId.value
    } catch (error: unknown) {
      messages.error =
        error instanceof Error ? error.message : 'Failed to load project allocations.'
      throw error
    } finally {
      loading.allocations = false
    }
  }

  async function assignMemberToProject(
    userId: string,
    projectId: string,
    role: ProjectMembershipRole,
  ) {
    const key = projectRoleKey(userId, projectId)
    savingProjectRoleByKey[key] = true
    messages.error = ''
    messages.success = ''

    try {
      const membership = await teamApi.assignTeamMemberToProject(userId, projectId, { role })
      upsertMembership(membership)
      loaded.allocations = true
      messages.success = 'Project allocation saved.'
      return membership
    } catch (error: unknown) {
      messages.error = error instanceof Error ? error.message : 'Failed to assign project.'
      throw error
    } finally {
      savingProjectRoleByKey[key] = false
    }
  }

  async function updateProjectRole(userId: string, projectId: string, role: ProjectMembershipRole) {
    const key = projectRoleKey(userId, projectId)
    savingProjectRoleByKey[key] = true
    messages.error = ''
    messages.success = ''

    try {
      const membership = await teamApi.updateTeamMemberProjectRole(userId, projectId, { role })
      upsertMembership(membership)
      messages.success = 'Project role updated.'
      return membership
    } catch (error: unknown) {
      messages.error = error instanceof Error ? error.message : 'Failed to update project role.'
      throw error
    } finally {
      savingProjectRoleByKey[key] = false
    }
  }

  async function removeMemberFromProject(userId: string, projectId: string) {
    const key = projectRoleKey(userId, projectId)
    savingProjectRoleByKey[key] = true
    messages.error = ''
    messages.success = ''

    try {
      await teamApi.removeTeamMemberFromProject(userId, projectId)
      membershipsByProjectId[projectId] = (membershipsByProjectId[projectId] ?? []).filter(
        (membership) => membership.userId !== userId,
      )
      messages.success = 'Project allocation removed.'
    } catch (error: unknown) {
      messages.error =
        error instanceof Error ? error.message : 'Failed to remove project allocation.'
      throw error
    } finally {
      savingProjectRoleByKey[key] = false
    }
  }

  async function updateGlobalRole(userId: string, role: GlobalRole) {
    savingGlobalRoleByUserId[userId] = true
    messages.error = ''
    messages.success = ''

    try {
      const response = await teamApi.updateTeamMemberGlobalRole(userId, { role })
      if (directory.value) {
        directory.value = {
          ...directory.value,
          members: directory.value.members.map((member) =>
            member.userId === userId ? { ...member, globalRole: response.role } : member,
          ),
        }
      }
      messages.success = 'Global role updated.'
      return response
    } catch (error: unknown) {
      messages.error = error instanceof Error ? error.message : 'Failed to update global role.'
      throw error
    } finally {
      savingGlobalRoleByUserId[userId] = false
    }
  }

  function upsertMembership(membership: ProjectMembership) {
    const current = membershipsByProjectId[membership.projectId] ?? []
    const index = current.findIndex((item) => item.userId === membership.userId)
    membershipsByProjectId[membership.projectId] =
      index >= 0
        ? current.map((item, itemIndex) => (itemIndex === index ? membership : item))
        : [...current, membership]
  }

  function projectRoleKey(userId: string, projectId: string) {
    return `${userId}:${projectId}`
  }

  return {
    directory,
    members,
    memberCount,
    totalOpenTasks,
    projects,
    activeProjects,
    allocationsByUserId,
    loading,
    savingProjectRoleByKey,
    savingGlobalRoleByUserId,
    loaded,
    messages,
    fetchDirectory,
    fetchAllocations,
    assignMemberToProject,
    updateProjectRole,
    removeMemberFromProject,
    updateGlobalRole,
    projectRoleKey,
  }
})
