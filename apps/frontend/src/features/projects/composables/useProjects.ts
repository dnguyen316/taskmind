import { computed, reactive, ref } from 'vue'
import * as projectsApi from '../api/projectsApi'

interface Project {
  id: string
  archived?: boolean | null
  [key: string]: unknown
}

interface Member {
  id: string
  [key: string]: unknown
}

interface ProjectFilters {
  includeArchived: boolean
}

export function useProjects() {
  const projects = ref<Project[]>([])
  const selectedProject = ref<Project | null>(null)
  const members = ref<Member[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const errorMessage = ref('')
  const successMessage = ref('')

  const filters = reactive<ProjectFilters>({
    includeArchived: false,
  })

  const activeProjectsCount = computed(() => projects.value.filter((project) => !project.archived).length)
  const archivedProjectsCount = computed(() => projects.value.filter((project) => Boolean(project.archived)).length)

  async function fetchProjects() {
    loading.value = true
    errorMessage.value = ''

    try {
      const response = await projectsApi.fetchProjects({ includeArchived: filters.includeArchived })
      projects.value = Array.isArray(response) ? response : []
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
      const project = await projectsApi.fetchProject(projectId)
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

  async function submitProject(payload: Record<string, unknown>) {
    saving.value = true
    errorMessage.value = ''
    successMessage.value = ''

    try {
      const created = await projectsApi.submitProject(payload)
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

  async function saveProject(projectId: string, payload: Record<string, unknown>) {
    saving.value = true
    errorMessage.value = ''
    successMessage.value = ''

    const originalProjects = [...projects.value]
    const targetIndex = projects.value.findIndex((project) => project.id === projectId)

    if (targetIndex >= 0) {
      projects.value[targetIndex] = { ...projects.value[targetIndex], ...payload }
    }

    try {
      const updated = await projectsApi.saveProject(projectId, payload)
      projects.value = projects.value.map((project) => (project.id === projectId ? { ...project, ...updated } : project))

      if (selectedProject.value?.id === projectId) {
        selectedProject.value = { ...selectedProject.value, ...updated }
      }

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
    errorMessage.value = ''
    successMessage.value = ''

    const originalProjects = [...projects.value]
    const originalSelectedProject = selectedProject.value ? { ...selectedProject.value } : null

    projects.value = projects.value.map((project) => (project.id === projectId ? { ...project, archived: true } : project))

    if (selectedProject.value?.id === projectId) {
      selectedProject.value = { ...selectedProject.value, archived: true }
    }

    try {
      await projectsApi.archiveProjectById(projectId)
      successMessage.value = 'Project archived.'
    } catch (error: unknown) {
      projects.value = originalProjects
      selectedProject.value = originalSelectedProject
      errorMessage.value = error instanceof Error ? error.message : 'Failed to archive project.'
      throw error
    }
  }

  async function fetchMembers(projectId: string) {
    loading.value = true
    errorMessage.value = ''

    try {
      const response = await projectsApi.fetchMembers(projectId)
      members.value = Array.isArray(response) ? response : []
      return members.value
    } catch (error: unknown) {
      errorMessage.value = error instanceof Error ? error.message : 'Failed to load members.'
      members.value = []
      return []
    } finally {
      loading.value = false
    }
  }

  async function addMember(projectId: string, payload: Record<string, unknown>) {
    saving.value = true
    errorMessage.value = ''
    successMessage.value = ''

    try {
      const created = await projectsApi.addMember(projectId, payload)
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
    members.value = members.value.filter((member) => member.id !== memberId)

    try {
      await projectsApi.removeMember(projectId, memberId)
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
