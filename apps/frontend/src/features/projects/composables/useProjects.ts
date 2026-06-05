import { computed, reactive, ref } from 'vue'
import * as projectsApi from '../api/projectsApi'
import type { AddProjectMemberPayload, CreateProjectPayload, Project, ProjectMembership, UpdateProjectPayload } from '../types'

interface ProjectFilters {
  includeArchived: boolean
}

export function useProjects() {
  const projects = ref<Project[]>([])
  const selectedProject = ref<Project | null>(null)
  const members = ref<ProjectMembership[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const errorMessage = ref('')
  const successMessage = ref('')

  const filters = reactive<ProjectFilters>({
    includeArchived: false,
  })

  const activeProjectsCount = computed(() => projects.value.filter((project) => !project.archivedAt).length)
  const archivedProjectsCount = computed(() => projects.value.filter((project) => Boolean(project.archivedAt)).length)

  function mergeProjectState(projectId: string, changes: Partial<Project>) {
    projects.value = projects.value.map((project) => (project.id === projectId ? { ...project, ...changes } : project))

    if (selectedProject.value?.id === projectId) {
      selectedProject.value = { ...selectedProject.value, ...changes }
    }
  }

  async function fetchProjects() {
    loading.value = true
    errorMessage.value = ''

    try {
      projects.value = await projectsApi.listProjects({ includeArchived: filters.includeArchived })
      return projects.value
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load projects.'
      return []
    } finally {
      loading.value = false
    }
  }

  async function fetchProject(projectId: string) {
    loading.value = true
    errorMessage.value = ''

    try {
      const project = await projectsApi.getProject(projectId)
      selectedProject.value = project ?? null
      return selectedProject.value
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load project.'
      selectedProject.value = null
      return null
    } finally {
      loading.value = false
    }
  }

  async function submitProject(payload: CreateProjectPayload) {
    saving.value = true
    errorMessage.value = ''
    successMessage.value = ''

    try {
      const created = await projectsApi.createProject(payload)
      successMessage.value = 'Project created.'
      await fetchProjects()
      return created
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to create project.'
      throw error
    } finally {
      saving.value = false
    }
  }

  async function saveProject(projectId: string, payload: UpdateProjectPayload) {
    saving.value = true
    errorMessage.value = ''
    successMessage.value = ''

    const originalProjects = [...projects.value]
    const targetIndex = projects.value.findIndex((project) => project.id === projectId)

    if (targetIndex >= 0) {
      projects.value[targetIndex] = { ...projects.value[targetIndex], ...payload }
    }

    try {
      const updated = await projectsApi.updateProject(projectId, payload)
      mergeProjectState(projectId, updated)

      successMessage.value = 'Project saved.'
      return updated
    } catch (error: unknown) {
      projects.value = originalProjects
      errorMessage.value = error instanceof Error ? error.message : 'Failed to save project.'
      throw error
    } finally {
      saving.value = false
    }
  }

  async function archiveProjectById(projectId: string) {
    saving.value = true
    errorMessage.value = ''
    successMessage.value = ''

    const originalProjects = [...projects.value]
    const originalSelectedProject = selectedProject.value ? { ...selectedProject.value } : null

    mergeProjectState(projectId, { archivedAt: new Date().toISOString() })

    try {
      const updated = await projectsApi.archiveProject(projectId)
      mergeProjectState(projectId, updated)
      successMessage.value = 'Project archived.'
    } catch (error: unknown) {
      projects.value = originalProjects
      selectedProject.value = originalSelectedProject
      errorMessage.value = error instanceof Error ? error.message : 'Failed to archive project.'
      throw error
    } finally {
      saving.value = false
    }
  }

  async function fetchMembers(projectId: string) {
    loading.value = true
    errorMessage.value = ''

    try {
      members.value = await projectsApi.listProjectMembers(projectId)
      return members.value
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load members.'
      members.value = []
      return []
    } finally {
      loading.value = false
    }
  }

  async function addMember(projectId: string, payload: AddProjectMemberPayload) {
    saving.value = true
    errorMessage.value = ''
    successMessage.value = ''

    try {
      const created = await projectsApi.addProjectMember(projectId, payload)
      members.value = [...members.value, created]
      successMessage.value = 'Member added.'
      return created
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to add member.'
      throw error
    } finally {
      saving.value = false
    }
  }

  async function removeMember(projectId: string, memberId: string) {
    saving.value = true
    errorMessage.value = ''
    successMessage.value = ''

    const originalMembers = [...members.value]
    members.value = members.value.filter((member) => member.userId !== memberId)

    try {
      await projectsApi.removeProjectMember(projectId, memberId)
      successMessage.value = 'Member removed.'
    } catch (error: unknown) {
      members.value = originalMembers
      errorMessage.value = error instanceof Error ? error.message : 'Failed to remove member.'
      throw error
    } finally {
      saving.value = false
    }
  }

  return {
    projects,
    selectedProject,
    members,
    loading,
    saving,
    errorMessage,
    successMessage,
    filters,
    activeProjectsCount,
    archivedProjectsCount,
    fetchProjects,
    fetchProject,
    submitProject,
    saveProject,
    archiveProjectById,
    fetchMembers,
    addMember,
    removeMember,
  }
}
