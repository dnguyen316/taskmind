<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../../tasks/components/AppLayout.vue'
import { useAuthStore } from '../../../stores/auth'
import { listProjectMembers } from '../../projects/api/projectsApi'
import {
  archiveAiWorkflowTemplate,
  createAiWorkflowTemplate,
  listAiWorkflowTemplates,
  updateAiWorkflowTemplate,
} from '../api/aiWorkflowTemplatesApi'
import AiWorkflowTemplateForm from '../components/AiWorkflowTemplateForm.vue'
import type { AiWorkflowTemplate, AiWorkflowTemplatePayload } from '../types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const projectId = computed(() => String(route.params.projectId ?? '').trim())
const templates = ref<AiWorkflowTemplate[]>([])
const selected = ref<AiWorkflowTemplate | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const fatal = ref('')
const notice = ref('')
const canManage = ref(false)
const empty = computed(() => !loading.value && !error.value && templates.value.length === 0)
async function load() {
  if (!projectId.value) return
  loading.value = true
  error.value = ''
  fatal.value = ''
  try {
    const [members, list] = await Promise.all([
      listProjectMembers(projectId.value),
      listAiWorkflowTemplates(projectId.value),
    ])
    const mine = members.find((m) => m.userId === auth.currentUserId)
    canManage.value = ['OWNER', 'ADMIN'].includes(mine?.role ?? '')
    templates.value = list
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Unable to load workflow templates.'
    if (!templates.value.length) fatal.value = error.value
  } finally {
    loading.value = false
  }
}
async function save(payload: AiWorkflowTemplatePayload) {
  saving.value = true
  error.value = ''
  try {
    const saved = selected.value
      ? await updateAiWorkflowTemplate(selected.value.id, payload)
      : await createAiWorkflowTemplate(projectId.value, payload)
    const index = templates.value.findIndex((template) => template.id === saved.id)
    if (index >= 0) templates.value[index] = saved
    else templates.value.unshift(saved)
    selected.value = null
    notice.value = 'Workflow template saved.'
  } catch (e) {
    error.value =
      e instanceof Error ? e.message : 'Template save failed. You can correct the form and retry.'
  } finally {
    saving.value = false
  }
}
async function archiveTemplate(template: AiWorkflowTemplate) {
  try {
    await archiveAiWorkflowTemplate(template.id)
    templates.value = templates.value.filter((item) => item.id !== template.id)
    notice.value = 'Workflow template archived.'
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Archive failed. Please retry.'
  }
}
onMounted(load)
watch(projectId, load)
</script>
<template>
  <AppLayout
    ><section class="workflow-page">
      <a-card title="AI workflow templates" class="surface-card"
        ><a-space direction="vertical" style="width: 100%" size="middle"
          ><a-space
            ><a-button @click="router.push({ name: 'project-detail', params: { id: projectId } })"
              >Back to project</a-button
            ><a-tag :color="canManage ? 'green' : 'orange'">{{
              canManage ? 'Admin management enabled' : 'Read-only project member'
            }}</a-tag></a-space
          ><a-alert
            v-if="fatal"
            type="error"
            show-icon
            message="Fatal error"
            :description="fatal" /><a-alert
            v-else-if="error"
            type="warning"
            show-icon
            message="Recoverable error"
            :description="error" /><a-alert
            v-else-if="notice"
            type="success"
            show-icon
            :message="notice" /><a-spin v-if="loading" tip="Loading templates..." /><a-empty
            v-else-if="empty"
            description="No workflow templates yet." /><a-list
            v-else
            bordered
            :data-source="templates"
            ><template #renderItem="{ item }"
              ><a-list-item
                ><template #actions
                  ><a-button size="small" :disabled="!canManage" @click="selected = item"
                    >Edit</a-button
                  ><a-button
                    danger
                    size="small"
                    :disabled="!canManage"
                    @click="archiveTemplate(item)"
                    >Archive</a-button
                  ></template
                ><a-list-item-meta
                  :title="item.name"
                  :description="item.description || 'No description'"
                  ><template #avatar
                    ><a-tag>{{ item.workflowType }}</a-tag></template
                  ></a-list-item-meta
                ><a-space wrap
                  ><a-tag>{{ item.approvalPolicy }}</a-tag
                  ><a-tag v-if="item.autoApproveReadOnly" color="blue"
                    >read-only auto</a-tag
                  ></a-space
                ></a-list-item
              ></template
            ></a-list
          ><a-alert
            v-if="!canManage"
            type="info"
            show-icon
            message="Only project owners/admins can create or edit templates." /><AiWorkflowTemplateForm
            v-else
            :template="selected"
            :saving="saving"
            @save="save"
            @cancel="selected = null" /></a-space
      ></a-card></section
  ></AppLayout>
</template>
<style scoped>
.workflow-page {
  display: grid;
  gap: 16px;
}
.surface-card {
  border-radius: 18px;
}
</style>
