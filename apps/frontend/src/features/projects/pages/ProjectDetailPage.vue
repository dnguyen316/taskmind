<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../tasks/components/AppLayout.vue'
import ProjectMembersPanel from '../components/ProjectMembersPanel.vue'
import GitHubRepositoryLinksPanel from '../../integrations/components/GitHubRepositoryLinksPanel.vue'
import { useAuthStore } from '../../../stores/auth'
import { getProjectHealth } from '../api/projectsApi'
import { useProjects } from '../composables/useProjects'
import type { ProjectHealth } from '../types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const projectId = computed(() => String(route.params.id ?? '').trim())
const projectHealth = ref<ProjectHealth | null>(null)
const healthLoading = ref(false)
const healthError = ref('')
const {
  selectedProject,
  members,
  loading,
  saving,
  errorMessage,
  fetchProject,
  fetchMembers,
  addMember,
  removeMember,
} = useProjects()

async function loadProject() {
  if (!projectId.value) {
    return
  }

  await fetchProject(projectId.value)
  await fetchMembers(projectId.value)
  await loadProjectHealth()
}

async function loadProjectHealth() {
  if (!projectId.value) {
    projectHealth.value = null
    return
  }

  healthLoading.value = true
  healthError.value = ''

  try {
    projectHealth.value = await getProjectHealth(projectId.value)
  } catch (error: unknown) {
    healthError.value = error instanceof Error ? error.message : 'Failed to load project health.'
  } finally {
    healthLoading.value = false
  }
}

function taskFilterLink(
  signal: 'all' | 'overdue' | 'blocked' | 'unassigned' | 'stale' | 'upcoming',
) {
  const query: Record<string, string> = { projectId: projectId.value }

  if (signal === 'overdue') query.overdueOnly = 'true'
  if (signal === 'blocked') query.blocked = 'true'
  if (signal === 'unassigned') query.unassigned = 'true'
  if (signal === 'stale') query.stale = 'true'
  if (signal === 'upcoming') query.sortBy = 'dueAt'

  return { name: 'tasks', query }
}

async function handleAddMember(payload: {
  userId: string
  role: 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER'
}) {
  await addMember(projectId.value, payload)
}

const canManageProject = computed(() => {
  const currentMembership = members.value.find(
    (member) => member.userId === authStore.currentUserId,
  )
  return currentMembership?.role === 'OWNER' || currentMembership?.role === 'ADMIN'
})

async function handleRemoveMember(memberId: string) {
  await removeMember(projectId.value, memberId)
}

onMounted(loadProject)
watch(projectId, loadProject)
</script>

<template>
  <AppLayout>
    <section class="project-detail-page">
      <a-card title="Project detail" class="surface-card">
        <a-space direction="vertical" style="width: 100%" size="middle">
          <a-space>
            <a-button @click="router.push({ name: 'projects-dashboard' })"
              >Back to projects</a-button
            >
            <a-button @click="router.push({ name: 'project-ai-workflows', params: { projectId } })"
              >AI workflows</a-button
            >
            <span class="project-id" v-if="projectId">ID: {{ projectId }}</span>
          </a-space>

          <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />

          <a-spin :spinning="loading">
            <a-descriptions bordered :column="1" size="small" title="Metadata">
              <a-descriptions-item label="Name">{{
                selectedProject?.name || '—'
              }}</a-descriptions-item>
              <a-descriptions-item label="Owner">{{
                selectedProject?.ownerUserId || '—'
              }}</a-descriptions-item>
              <a-descriptions-item label="Status">{{
                selectedProject?.archivedAt ? 'ARCHIVED' : 'ACTIVE'
              }}</a-descriptions-item>
              <a-descriptions-item label="Description">{{
                selectedProject?.description || 'No description'
              }}</a-descriptions-item>
            </a-descriptions>
          </a-spin>

          <a-card title="Project health" class="surface-card health-card">
            <a-alert v-if="healthError" type="error" show-icon :message="healthError" />
            <a-spin :spinning="healthLoading">
              <a-space direction="vertical" style="width: 100%" size="middle">
                <a-row :gutter="[12, 12]">
                  <a-col :xs="24" :sm="12" :lg="8">
                    <router-link :to="taskFilterLink('all')" class="health-signal">
                      <a-statistic
                        title="Completion"
                        :value="projectHealth?.completionPercentage ?? 0"
                        suffix="%"
                      />
                    </router-link>
                  </a-col>
                  <a-col :xs="24" :sm="12" :lg="8">
                    <router-link :to="taskFilterLink('overdue')" class="health-signal">
                      <a-statistic
                        title="Overdue tasks"
                        :value="projectHealth?.overdueTaskCount ?? 0"
                      />
                    </router-link>
                  </a-col>
                  <a-col :xs="24" :sm="12" :lg="8">
                    <router-link :to="taskFilterLink('blocked')" class="health-signal">
                      <a-statistic
                        title="Blocked tasks"
                        :value="projectHealth?.blockedTaskCount ?? 0"
                      />
                    </router-link>
                  </a-col>
                  <a-col :xs="24" :sm="12" :lg="8">
                    <router-link :to="taskFilterLink('unassigned')" class="health-signal">
                      <a-statistic
                        title="Unassigned tasks"
                        :value="projectHealth?.unassignedTaskCount ?? 0"
                      />
                    </router-link>
                  </a-col>
                  <a-col :xs="24" :sm="12" :lg="8">
                    <router-link :to="taskFilterLink('stale')" class="health-signal">
                      <a-statistic
                        title="Stale tasks"
                        :value="projectHealth?.staleTaskCount ?? 0"
                      />
                    </router-link>
                  </a-col>
                  <a-col :xs="24" :sm="12" :lg="8">
                    <router-link :to="taskFilterLink('upcoming')" class="health-signal">
                      <a-statistic
                        title="Upcoming deadline risk"
                        :value="projectHealth?.upcomingDeadlineRiskCount ?? 0"
                      />
                    </router-link>
                  </a-col>
                </a-row>
                <a-alert
                  type="info"
                  show-icon
                  :message="
                    projectHealth?.narrative ?? 'Project health will appear after tasks are loaded.'
                  "
                />
                <a-list
                  size="small"
                  bordered
                  :data-source="projectHealth?.workloadByAssignee ?? []"
                >
                  <template #header>Workload by assignee</template>
                  <template #renderItem="{ item }">
                    <a-list-item>
                      <router-link
                        :to="{ name: 'tasks', query: { projectId, assigneeId: item.assigneeId } }"
                      >
                        {{ item.assigneeId }} — {{ item.activeTaskCount }} active tasks
                      </router-link>
                    </a-list-item>
                  </template>
                  <template #emptyText>No assigned active work.</template>
                </a-list>
              </a-space>
            </a-spin>
          </a-card>

          <GitHubRepositoryLinksPanel
            v-if="projectId"
            :project-id="projectId"
            :can-manage="canManageProject"
          />

          <ProjectMembersPanel
            :members="members"
            :loading="loading"
            :saving="saving"
            :error-message="errorMessage"
            @add-member="handleAddMember"
            @remove-member="handleRemoveMember"
          />
        </a-space>
      </a-card>
    </section>
  </AppLayout>
</template>

<style scoped>
.project-detail-page {
  display: grid;
  gap: 16px;
}

.surface-card {
  border-radius: 18px;
}

.health-card {
  border-radius: 18px;
}

.health-signal {
  display: block;
  padding: 12px;
  border: 1px solid var(--tm-border-subtle);
  border-radius: 14px;
  background: var(--tm-bg-surface);
}

.project-id {
  margin: 0;
  color: var(--tm-text-muted);
  font-weight: 600;
}
</style>
