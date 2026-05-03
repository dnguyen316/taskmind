<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRouter } from 'vue-router'

type ProjectStatus = 'ACTIVE' | 'ARCHIVED'

type ProjectRecord = {
  id: string
  name: string
  description: string
  owner: string
  status: ProjectStatus
}

const router = useRouter()

const createForm = reactive({
  name: '',
  description: '',
  owner: '',
})

const projects = reactive<ProjectRecord[]>([
  {
    id: crypto.randomUUID(),
    name: 'TaskMind Core Platform',
    description: 'Foundational project for workflow orchestration and shared APIs.',
    owner: 'Platform Team',
    status: 'ACTIVE',
  },
])

const activeProjects = computed(() => projects.filter((project) => project.status === 'ACTIVE'))
const archivedProjects = computed(() => projects.filter((project) => project.status === 'ARCHIVED'))

function createProject() {
  const name = createForm.name.trim()
  if (!name) {
    return
  }

  projects.unshift({
    id: crypto.randomUUID(),
    name,
    description: createForm.description.trim(),
    owner: createForm.owner.trim() || 'Unassigned',
    status: 'ACTIVE',
  })

  createForm.name = ''
  createForm.description = ''
  createForm.owner = ''
}

function archiveProject(projectId: string) {
  const targetProject = projects.find((project) => project.id === projectId)
  if (!targetProject) {
    return
  }

  targetProject.status = 'ARCHIVED'
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
        <a-statistic title="Active projects" :value="activeProjects.length" />
        <a-statistic title="Archived projects" :value="archivedProjects.length" />
      </a-space>
    </section>

    <a-card title="Create project" class="surface-card">
      <a-form layout="vertical" @submit.prevent="createProject">
        <a-row :gutter="12">
          <a-col :xs="24" :md="8">
            <a-form-item label="Project name" required>
              <a-input v-model:value="createForm.name" placeholder="TaskMind Mobile" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="8">
            <a-form-item label="Owner">
              <a-input v-model:value="createForm.owner" placeholder="Team or owner" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="8">
            <a-form-item label="Description">
              <a-input v-model:value="createForm.description" placeholder="Short summary" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-button type="primary" html-type="submit">Create project</a-button>
      </a-form>
    </a-card>

    <a-card title="Project list" class="surface-card">
      <a-empty v-if="activeProjects.length === 0" description="No active projects." />
      <a-list v-else item-layout="horizontal" :data-source="activeProjects">
        <template #renderItem="{ item }">
          <a-list-item>
            <template #actions>
              <a-button type="link" @click="router.push({ name: 'project-detail', params: { id: item.id } })">View</a-button>
              <a-button danger type="link" @click="archiveProject(item.id)">Archive</a-button>
            </template>
            <a-list-item-meta :title="item.name" :description="item.description || 'No description'">
              <template #avatar>
                <a-avatar>{{ item.name.slice(0, 1).toUpperCase() }}</a-avatar>
              </template>
            </a-list-item-meta>
            <span class="owner-pill">{{ item.owner }}</span>
          </a-list-item>
        </template>
      </a-list>
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
