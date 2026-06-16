<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { FolderOpenOutlined, PlusCircleOutlined } from '@ant-design/icons-vue'
import { useAuthStore } from '../../../stores/auth'
import AppLayout from '../../tasks/components/AppLayout.vue'
import type { CreateProjectPayload, Project } from '../types'
import ProjectCreateForm from '../components/ProjectCreateForm.vue'
import { useProjects } from '../composables/useProjects'

const router = useRouter()
const authStore = useAuthStore()
const {
  projects,
  loading,
  saving,
  archivingProjectIds,
  errorMessage,
  successMessage,
  activeProjectsCount,
  archivedProjectsCount,
  fetchProjects,
  submitProject,
  archiveProjectById,
} = useProjects()

const successSignal = ref(0)
const activeProjects = computed<Project[]>(() =>
  projects.value.filter((project) => !project.archivedAt),
)
const archiveButtonsDisabled = computed(() => saving.value)

function isArchivingProject(projectId: string) {
  return Boolean(archivingProjectIds.value[projectId])
}

async function confirmArchiveProject(projectId: string) {
  await archiveProjectById(projectId)
}

onMounted(() => {
  void fetchProjects({ force: true })
})

async function createProject(payload: CreateProjectPayload) {
  await submitProject(payload)
  successSignal.value += 1
}
</script>

<template>
  <AppLayout>
    <section class="projects-hero tm-card-surface">
      <div class="hero-copy">
        <p class="eyebrow">Projects</p>
        <h1>Plan work by outcome</h1>
        <p>
          Track active initiatives, create task-ready project keys, and archive completed
          workstreams without leaving the TaskMind shell.
        </p>
      </div>
      <div class="hero-stats">
        <a-statistic title="Active" :value="activeProjectsCount" />
        <a-statistic title="Archived" :value="archivedProjectsCount" />
      </div>
    </section>

    <div class="projects-grid">
      <ProjectCreateForm
        :saving="saving"
        :success-signal="successSignal"
        :current-user-label="authStore.currentUserDisplayName"
        @submit="createProject"
      />

      <a-card class="project-list-card tm-card-surface" :bordered="false">
        <template #title>
          <span class="card-title"><FolderOpenOutlined /> Active projects</span>
        </template>
        <div v-if="errorMessage || successMessage" class="project-list-feedback">
          <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />
          <a-alert v-else type="success" show-icon :message="successMessage" />
        </div>
        <a-spin :spinning="loading">
          <a-empty v-if="activeProjects.length === 0" description="No active projects yet.">
            <template #image><PlusCircleOutlined class="empty-icon" /></template>
          </a-empty>
          <a-list v-else item-layout="vertical" :data-source="activeProjects" class="project-list">
            <template #renderItem="{ item }: { item: Project }">
              <a-list-item class="project-list-item">
                <template #actions>
                  <a-button
                    type="link"
                    @click="router.push({ name: 'project-detail', params: { id: item.id } })"
                  >
                    View details
                  </a-button>
                  <a-popconfirm
                    title="Archive this project?"
                    ok-text="Archive"
                    cancel-text="Cancel"
                    ok-type="danger"
                    :disabled="archiveButtonsDisabled"
                    @confirm="confirmArchiveProject(item.id)"
                  >
                    <a-button
                      danger
                      size="small"
                      :loading="isArchivingProject(item.id)"
                      :disabled="archiveButtonsDisabled && !isArchivingProject(item.id)"
                    >
                      Archive project
                    </a-button>
                  </a-popconfirm>
                </template>
                <a-list-item-meta :description="item.description || 'No description provided.'">
                  <template #title>
                    <div class="project-title-row">
                      <span>{{ item.name }}</span>
                      <a-tag color="blue">{{ item.key }}</a-tag>
                    </div>
                  </template>
                  <template #avatar>
                    <a-avatar class="project-avatar">
                      {{
                        String(item.name || item.key || item.id)
                          .slice(0, 1)
                          .toUpperCase()
                      }}
                    </a-avatar>
                  </template>
                </a-list-item-meta>
                <p class="owner-pill">Owner {{ item.ownerUserId || 'authenticated user' }}</p>
              </a-list-item>
            </template>
          </a-list>
        </a-spin>
      </a-card>
    </div>
  </AppLayout>
</template>

<style scoped>
.projects-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 24px;
  align-items: center;
  padding: 26px;
  border-radius: 24px;
  background: linear-gradient(
    135deg,
    color-mix(in srgb, var(--tm-primary) 12%, transparent),
    color-mix(in srgb, var(--tm-accent-teal) 10%, transparent)
  );
}

.eyebrow {
  margin: 0;
  color: var(--tm-accent-blue-strong);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.hero-copy h1 {
  margin: 6px 0;
  color: var(--tm-text);
  font-size: clamp(1.8rem, 4vw, 2.4rem);
}

.hero-copy p:not(.eyebrow) {
  max-width: 660px;
  margin: 0;
  color: var(--tm-text-muted);
}

.hero-stats {
  display: flex;
  gap: 22px;
  padding: 14px 18px;
  border: 1px solid var(--tm-border);
  border-radius: 18px;
  background: color-mix(in srgb, var(--tm-card-bg) 72%, transparent);
}

.projects-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.85fr) minmax(0, 1.15fr);
  gap: 18px;
}

.project-list-card {
  border-radius: 22px;
}

.project-list-feedback {
  margin-bottom: 16px;
}

.card-title {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.project-list :deep(.ant-list-item) {
  padding-inline: 0;
}

.project-list-item {
  border-color: var(--tm-border);
}

.project-title-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.project-avatar {
  background: var(--tm-accent-blue);
}

.owner-pill {
  margin: 10px 0 0 48px;
  color: var(--tm-text-muted);
  font-size: 12px;
  font-weight: 600;
}

.empty-icon {
  color: var(--tm-accent-blue);
  font-size: 44px;
}

@media (max-width: 1000px) {
  .projects-hero,
  .projects-grid {
    grid-template-columns: 1fr;
  }

  .hero-stats {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
