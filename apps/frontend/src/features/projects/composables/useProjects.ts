import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useAssigneesStore } from '../../../stores/assignees'
import { useProjectsStore } from '../../../stores/projects'

export function useProjects() {
  const projectsStore = useProjectsStore()
  const assigneesStore = useAssigneesStore()
  const { projects, selectedProject, filters, activeProjectsCount, archivedProjectsCount } = storeToRefs(projectsStore)

  const members = computed(() => {
    const projectId = selectedProject.value?.id
    return projectId ? assigneesStore.membersForProject(projectId) : []
  })

  const loading = computed(() => projectsStore.loading.list || projectsStore.loading.detail)
  const saving = computed(() => projectsStore.saving.project || Object.values(assigneesStore.savingByProjectId).some(Boolean))
  const errorMessage = computed(() => projectsStore.messages.error || assigneesStore.messages.error)
  const successMessage = computed(() => projectsStore.messages.success || assigneesStore.messages.success)

  async function fetchProject(projectId: string) {
    return projectsStore.fetchProject(projectId, { force: true })
  }

  async function fetchMembers(projectId: string) {
    await projectsStore.fetchProject(projectId)
    return assigneesStore.fetchMembers(projectId, { force: true })
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
    fetchProjects: projectsStore.fetchProjects,
    fetchProject,
    submitProject: projectsStore.submitProject,
    saveProject: projectsStore.saveProject,
    archiveProjectById: projectsStore.archiveProjectById,
    fetchMembers,
    addMember: assigneesStore.addMember,
    removeMember: assigneesStore.removeMember,
  }
}
