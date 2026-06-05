<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ProjectCreateForm from '../components/ProjectCreateForm.vue'
import { useProjects } from '../composables/useProjects'

const router = useRouter()
const {
  projects,
  loading,
  saving,
  errorMessage,
  successMessage,
  activeProjectsCount,
  archivedProjectsCount,
  fetchProjects,
  submitProject,
  archiveProjectById,
} = useProjects()

const successSignal = ref(0)
const activeProjects = computed(() => projects.value.filter((project) => !project.archived && project.status !== 'ARCHIVED'))

onMounted(() => {
  void fetchProjects({ force: true })
})

async function createProject(payload: Record<string, unknown>) {
  await submitProject(payload)
  successSignal.value += 1
}
</script>

<template>
  <main class="projects-page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Projects</p>
        <h1>Project Dashboard</h1>
        <p class="hero-copy">Track active initiatives, spin up new projects, and archive completed workstreams.</p>
      </div>
      <a-space size="middle" class="hero-stats">
        <a-statistic title="Active projects" :value="activeProjectsCount" />
        <a-statistic title="Archived projects" :value="archivedProjectsCount" />
      </a-space>
    </section>

    <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />
    <a-alert v-if="successMessage" type="success" show-icon :message="successMessage" />

    <ProjectCreateForm :saving="saving" :success-signal="successSignal" @submit="createProject" />

    <a-card title="Project list" class="surface-card">
      <a-spin :spinning="loading">
        <a-empty v-if="activeProjects.length === 0" description="No active projects." />
        <a-list v-else item-layout="horizontal" :data-source="activeProjects">
          <template #renderItem="{ item }">
            <a-list-item>
              <template #actions>
                <a-button type="link" @click="router.push({ name: 'project-detail', params: { id: item.id } })">View</a-button>
                <a-button danger type="link" @click="archiveProjectById(item.id)">Archive</a-button>
              </template>
              <a-list-item-meta :title="item.name" :description="item.description || 'No description'">
                <template #avatar>
                  <a-avatar>{{ String(item.name || item.key || item.id).slice(0, 1).toUpperCase() }}</a-avatar>
                </template>
              </a-list-item-meta>
              <span class="owner-pill">{{ item.ownerUserId || 'Unassigned' }}</span>
            </a-list-item>
          </template>
        </a-list>
      </a-spin>
    </a-card>
  </main>
</template>

<style scoped>
.projects-page { min-height: 100vh; max-width: 1100px; margin: 0 auto; padding: 32px 20px 40px; display: grid; gap: 18px; }
.hero-card { background: linear-gradient(135deg, rgba(14, 116, 144, 0.13), rgba(2, 132, 199, 0.08)); border: 1px solid rgba(148, 163, 184, 0.35); border-radius: 20px; padding: 24px; display: grid; gap: 14px; }
.eyebrow { margin: 0; color: #0f766e; font-size: 12px; letter-spacing: 0.1em; text-transform: uppercase; font-weight: 700; }
.hero-card h1 { margin: 6px 0; font-size: clamp(1.6rem, 3.8vw, 2.15rem); }
.hero-copy { margin: 0; max-width: 620px; color: #334155; }
.surface-card { border-radius: 18px; }
.owner-pill { color: #64748b; font-weight: 600; }
</style>
