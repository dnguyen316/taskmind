<script setup lang="ts">
import { computed, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const projectId = computed(() => String(route.params.id ?? '').trim())

const metadata = reactive({
  name: 'TaskMind Core Platform',
  owner: 'Platform Team',
  status: 'ACTIVE',
  description: 'Foundational project for workflow orchestration and shared APIs.',
})

const newMember = reactive({ name: '', role: '' })
interface ProjectMemberRecord {
  id: string
  name: string
  role: string
}

const members = reactive<ProjectMemberRecord[]>([
  { id: crypto.randomUUID(), name: 'Avery Brooks', role: 'Product Manager' },
  { id: crypto.randomUUID(), name: 'Jules Park', role: 'Lead Engineer' },
])

function addMember() {
  const name = newMember.name.trim()
  if (!name) {
    return
  }

  members.push({
    id: crypto.randomUUID(),
    name,
    role: newMember.role.trim() || 'Contributor',
  })

  newMember.name = ''
  newMember.role = ''
}

function removeMember(memberId: string) {
  const index = members.findIndex((member) => member.id === memberId)
  if (index >= 0) {
    members.splice(index, 1)
  }
}
</script>

<template>
  <main class="project-detail-page">
    <a-card title="Project detail" class="surface-card">
      <a-space direction="vertical" style="width: 100%" size="middle">
        <a-space>
          <a-button @click="router.push({ name: 'projects-dashboard' })">Back to projects</a-button>
          <span class="project-id" v-if="projectId">ID: {{ projectId }}</span>
        </a-space>

        <a-descriptions bordered :column="1" size="small" title="Metadata">
          <a-descriptions-item label="Name">{{ metadata.name }}</a-descriptions-item>
          <a-descriptions-item label="Owner">{{ metadata.owner }}</a-descriptions-item>
          <a-descriptions-item label="Status">{{ metadata.status }}</a-descriptions-item>
          <a-descriptions-item label="Description">{{ metadata.description }}</a-descriptions-item>
        </a-descriptions>

        <a-card title="Members management" type="inner">
          <a-form layout="inline" @submit.prevent="addMember">
            <a-form-item>
              <a-input v-model:value="newMember.name" placeholder="Member name" />
            </a-form-item>
            <a-form-item>
              <a-input v-model:value="newMember.role" placeholder="Role" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" html-type="submit">Add member</a-button>
            </a-form-item>
          </a-form>

          <a-list class="members-list" :data-source="members" bordered>
            <template #renderItem="{ item }: { item: ProjectMemberRecord }">
              <a-list-item>
                <template #actions>
                  <a-button type="link" danger @click="removeMember(item.id)">Remove</a-button>
                </template>
                <a-list-item-meta :title="item.name" :description="item.role" />
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-space>
    </a-card>
  </main>
</template>

<style scoped>
.project-detail-page { min-height: 100vh; max-width: 1100px; margin: 0 auto; padding: 32px 20px 40px; }
.surface-card { border-radius: 18px; }
.project-id { margin: 0; font-weight: 600; color: #64748b; }
.members-list { margin-top: 14px; }
</style>
