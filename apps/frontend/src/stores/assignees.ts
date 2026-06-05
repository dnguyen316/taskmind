import { reactive } from 'vue'
import { defineStore } from 'pinia'
import * as projectsApi from '../features/projects/api/projectsApi'
import type { AddProjectMemberPayload, ProjectMembership } from '../features/projects/types'

export const useAssigneesStore = defineStore('assignees', () => {
  const membersByProjectId = reactive<Record<string, ProjectMembership[]>>({})
  const loadingByProjectId = reactive<Record<string, boolean>>({})
  const savingByProjectId = reactive<Record<string, boolean>>({})
  const messages = reactive({ error: '', success: '' })

  function membersForProject(projectId: string) {
    return membersByProjectId[projectId] ?? []
  }

  async function fetchMembers(projectId: string, { force = false }: { force?: boolean } = {}) {
    if (!force && membersByProjectId[projectId]) {
      return membersByProjectId[projectId]
    }

    loadingByProjectId[projectId] = true
    messages.error = ''

    try {
      const response = await projectsApi.listProjectMembers(projectId)
      membersByProjectId[projectId] = response
      return membersByProjectId[projectId]
    } catch (error: unknown) {
      messages.error = error instanceof Error ? error.message : 'Failed to load members.'
      membersByProjectId[projectId] = []
      return []
    } finally {
      loadingByProjectId[projectId] = false
    }
  }

  async function addMember(projectId: string, payload: AddProjectMemberPayload) {
    savingByProjectId[projectId] = true
    messages.error = ''
    messages.success = ''

    try {
      const created = await projectsApi.addProjectMember(projectId, payload)
      membersByProjectId[projectId] = [...membersForProject(projectId), created]
      messages.success = 'Member added.'
      return created
    } catch (error: unknown) {
      messages.error = error instanceof Error ? error.message : 'Failed to add member.'
      throw error
    } finally {
      savingByProjectId[projectId] = false
    }
  }

  async function removeMember(projectId: string, memberId: string) {
    savingByProjectId[projectId] = true
    messages.error = ''
    messages.success = ''
    const originalMembers = [...membersForProject(projectId)]
    membersByProjectId[projectId] = membersForProject(projectId).filter((member) => member.userId !== memberId)

    try {
      await projectsApi.removeProjectMember(projectId, memberId)
      messages.success = 'Member removed.'
    } catch (error: unknown) {
      membersByProjectId[projectId] = originalMembers
      messages.error = error instanceof Error ? error.message : 'Failed to remove member.'
      throw error
    } finally {
      savingByProjectId[projectId] = false
    }
  }

  return {
    membersByProjectId,
    loadingByProjectId,
    savingByProjectId,
    messages,
    membersForProject,
    fetchMembers,
    addMember,
    removeMember,
  }
})
