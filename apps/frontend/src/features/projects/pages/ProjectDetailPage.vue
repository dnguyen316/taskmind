<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ProjectMembersPanel from '../components/ProjectMembersPanel.vue'
import { useProjects } from '../composables/useProjects'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => String(route.params.id ?? '').trim())
const { selectedProject, members, loading, saving, errorMessage, fetchProject, fetchMembers, addMember, removeMember } = useProjects()


async function loadProject() {
  if (!projectId.value) {
    return
  }

  await fetchProject(projectId.value)
  await fetchMembers(projectId.value)
}

async function handleAddMember(payload: { userId: string; role: 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER' }) {
  await addMember(projectId.value, payload)
}

async function handleRemoveMember(memberId: string) {
  await removeMember(projectId.value, memberId)
}

onMounted(loadProject)
watch(projectId, loadProject)
</script>

<template>
  <main class="project-detail-page">
    <a-card title="Project detail" class="surface-card">
      <a-space direction="vertical" style="width: 100%" size="middle">
        <a-space>
          <a-button @click="router.push({ name: 'projects-dashboard' })">Back to projects</a-button>
          <span class="project-id" v-if="projectId">ID: {{ projectId }}</span>
        </a-space>

        <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />

        <a-spin :spinning="loading">
          <a-descriptions bordered :column="1" size="small" title="Metadata">
            <a-descriptions-item label="Name">{{ selectedProject?.name || '—' }}</a-descriptions-item>
            <a-descriptions-item label="Owner">{{ selectedProject?.ownerUserId || '—' }}</a-descriptions-item>
            <a-descriptions-item label="Status">{{ selectedProject?.archivedAt ? 'ARCHIVED' : 'ACTIVE' }}</a-descriptions-item>
            <a-descriptions-item label="Description">{{ selectedProject?.description || 'No description' }}</a-descriptions-item>
          </a-descriptions>
        </a-spin>

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
  </main>
</template>

<style scoped>
.project-detail-page { min-height: 100vh; max-width: 1100px; margin: 0 auto; padding: 32px 20px 40px; }
.surface-card { border-radius: 18px; }
.project-id { margin: 0; font-weight: 600; color: #64748b; }
</style>
