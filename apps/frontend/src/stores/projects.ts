import { computed, reactive } from 'vue'
import { defineStore } from 'pinia'
import * as projectsApi from '../features/projects/api/projectsApi'
import type { CreateProjectPayload, Project, UpdateProjectPayload } from '../features/projects/types'

export interface ProjectFilters {
  includeArchived: boolean
}

export const useProjectsStore = defineStore('projects', () => {
  const projects = reactive<Project[]>([])
  const selectedProjects = reactive<Record<string, Project>>({})
  const selectedProjectRef = reactive<{ id: string }>({ id: '' })
  const filters = reactive<ProjectFilters>({ includeArchived: false })
  const loading = reactive({ list: false, detail: false })
  const saving = reactive({ project: false })
  const messages = reactive({ error: '', success: '' })
  const loadedLists = reactive<Record<string, boolean>>({})

  const selectedProject = computed(() => (selectedProjectRef.id ? selectedProjects[selectedProjectRef.id] ?? null : null))
  const activeProjects = computed(() => projects.filter((project) => !project.archivedAt))
  const archivedProjects = computed(() => projects.filter((project) => Boolean(project.archivedAt)))
  const activeProjectsCount = computed(() => activeProjects.value.length)
  const archivedProjectsCount = computed(() => archivedProjects.value.length)

  function cacheKey(includeArchived = filters.includeArchived) {
    return includeArchived ? 'with-archived' : 'active-only'
  }

  function setProjects(nextProjects: Project[]) {
    projects.splice(0, projects.length, ...nextProjects)

    for (const project of nextProjects) {
      selectedProjects[project.id] = project
    }
  }

  function mergeProject(projectId: string, changes: Partial<Project>) {
    const targetIndex = projects.findIndex((project) => project.id === projectId)

    if (targetIndex >= 0) {
      projects[targetIndex] = { ...projects[targetIndex], ...changes }
    }

    if (selectedProjects[projectId]) {
      selectedProjects[projectId] = { ...selectedProjects[projectId], ...changes }
    }
  }

  async function fetchProjects({ force = false, includeArchived = filters.includeArchived }: { force?: boolean; includeArchived?: boolean } = {}) {
    filters.includeArchived = includeArchived
    const key = cacheKey(includeArchived)

    if (!force && loadedLists[key]) {
      return projects
    }

    loading.list = true
    messages.error = ''

    try {
      const response = await projectsApi.listProjects({ includeArchived })
      setProjects(response)
      loadedLists[key] = true
      return projects
    } catch (error: unknown) {
      messages.error = error instanceof Error ? error.message : 'Failed to load projects.'
      return []
    } finally {
      loading.list = false
    }
  }

  async function fetchProject(projectId: string, { force = false }: { force?: boolean } = {}) {
    if (!force && selectedProjects[projectId]) {
      selectedProjectRef.id = projectId
      return selectedProjects[projectId]
    }

    loading.detail = true
    messages.error = ''

    try {
      const project = await projectsApi.getProject(projectId)
      selectedProjects[projectId] = project
      selectedProjectRef.id = projectId
      mergeProject(projectId, project)
      return project
    } catch (error: unknown) {
      messages.error = error instanceof Error ? error.message : 'Failed to load project.'
      return null
    } finally {
      loading.detail = false
    }
  }

  async function submitProject(payload: CreateProjectPayload) {
    saving.project = true
    messages.error = ''
    messages.success = ''

    try {
      const created = await projectsApi.createProject(payload)
      projects.unshift(created)
      selectedProjects[created.id] = created
      selectedProjectRef.id = created.id
      loadedLists[cacheKey()] = true
      messages.success = 'Project created.'
      return created
    } catch (error: unknown) {
      messages.error = error instanceof Error ? error.message : 'Failed to create project.'
      throw error
    } finally {
      saving.project = false
    }
  }

  async function saveProject(projectId: string, payload: UpdateProjectPayload) {
    saving.project = true
    messages.error = ''
    messages.success = ''
    const originalProjects = projects.map((project) => ({ ...project }))
    const originalSelectedProject = selectedProjects[projectId] ? { ...selectedProjects[projectId] } : undefined

    mergeProject(projectId, payload)

    try {
      const updated = await projectsApi.updateProject(projectId, payload)
      mergeProject(projectId, updated)
      messages.success = 'Project saved.'
      return updated
    } catch (error: unknown) {
      setProjects(originalProjects)
      if (originalSelectedProject) {
        selectedProjects[projectId] = originalSelectedProject
      }
      messages.error = error instanceof Error ? error.message : 'Failed to save project.'
      throw error
    } finally {
      saving.project = false
    }
  }

  async function archiveProjectById(projectId: string) {
    saving.project = true
    messages.error = ''
    messages.success = ''
    const originalProjects = projects.map((project) => ({ ...project }))
    const originalSelectedProject = selectedProjects[projectId] ? { ...selectedProjects[projectId] } : undefined
    const archivedAt = new Date().toISOString()

    mergeProject(projectId, { archivedAt })

    try {
      const updated = await projectsApi.archiveProject(projectId)
      mergeProject(projectId, updated)
      messages.success = 'Project archived.'
    } catch (error: unknown) {
      setProjects(originalProjects)
      if (originalSelectedProject) {
        selectedProjects[projectId] = originalSelectedProject
      }
      messages.error = error instanceof Error ? error.message : 'Failed to archive project.'
      throw error
    } finally {
      saving.project = false
    }
  }

  return {
    projects,
    selectedProjects,
    selectedProject,
    filters,
    loading,
    saving,
    messages,
    activeProjects,
    archivedProjects,
    activeProjectsCount,
    archivedProjectsCount,
    fetchProjects,
    fetchProject,
    submitProject,
    saveProject,
    archiveProjectById,
    mergeProject,
  }
})
