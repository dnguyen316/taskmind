import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useAssigneesStore } from '../../../stores/assignees'
import { useProjectsStore } from '../../../stores/projects'
import type {
  AddProjectMemberPayload,
  CreateProjectPayload,
  ProjectMembership,
  UpdateProjectPayload,
} from '../types'

export function useProjects() {
  const projectsStore = useProjectsStore()
  const assigneesStore = useAssigneesStore()
  const {
    projects,
    selectedProject,
    filters,
    loading,
    saving,
    archivingByProjectId,
    messages,
    activeProjectsCount,
    archivedProjectsCount,
  } = storeToRefs(projectsStore)

  const currentProjectId = computed(() => selectedProject.value?.id ?? '')
  const members = computed<ProjectMembership[]>(() =>
    currentProjectId.value ? assigneesStore.membersForProject(currentProjectId.value) : [],
  )
  const loadingMembers = computed(() =>
    currentProjectId.value
      ? Boolean(assigneesStore.loadingByProjectId[currentProjectId.value])
      : false,
  )
  const savingMembers = computed(() =>
    currentProjectId.value
      ? Boolean(assigneesStore.savingByProjectId[currentProjectId.value])
      : false,
  )
  const loadingAny = computed(
    () => loading.value.list || loading.value.detail || loadingMembers.value,
  )
  const savingAny = computed(() => saving.value.project || savingMembers.value)
  const archivingProjectIds = computed(() => archivingByProjectId.value)
  const errorMessage = computed(() => messages.value.error || assigneesStore.messages.error)
  const successMessage = computed(() => messages.value.success || assigneesStore.messages.success)

  async function fetchProjects(options?: { force?: boolean; includeArchived?: boolean }) {
    return projectsStore.fetchProjects(options)
  }

  async function fetchProject(projectId: string, options?: { force?: boolean }) {
    return projectsStore.fetchProject(projectId, options)
  }

  async function submitProject(payload: CreateProjectPayload) {
    return projectsStore.submitProject(payload)
  }

  async function saveProject(projectId: string, payload: UpdateProjectPayload) {
    return projectsStore.saveProject(projectId, payload)
  }

  async function archiveProjectById(projectId: string) {
    return projectsStore.archiveProjectById(projectId)
  }

  async function fetchMembers(projectId: string, options?: { force?: boolean }) {
    return assigneesStore.fetchMembers(projectId, options)
  }

  async function addMember(projectId: string, payload: AddProjectMemberPayload) {
    return assigneesStore.addMember(projectId, payload)
  }

  async function removeMember(projectId: string, memberId: string) {
    return assigneesStore.removeMember(projectId, memberId)
  }

  return {
    projects,
    selectedProject,
    members,
    filters,
    loading: loadingAny,
    saving: savingAny,
    archivingProjectIds,
    errorMessage,
    successMessage,
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
