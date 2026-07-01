<script setup lang="ts">
import { computed, reactive, watch } from 'vue'
import {
  AI_TOOL_OPTIONS,
  type AiApprovalPolicy,
  type AiToolId,
  type AiWorkflowTemplate,
  type AiWorkflowTemplatePayload,
  type AiWorkflowType,
} from '../types'

const props = defineProps<{ template?: AiWorkflowTemplate | null; saving?: boolean }>()
const emit = defineEmits<{ save: [payload: AiWorkflowTemplatePayload]; cancel: [] }>()

const form = reactive({
  name: '',
  description: '',
  workflowType: 'TASK_RESOLUTION' as AiWorkflowType,
  templateBody: '',
  allowedTools: [] as AiToolId[],
  approvalPolicy: 'MANUAL' as AiApprovalPolicy,
  autoApproveReadOnly: true,
  requireApprovalForComments: true,
  requireApprovalForBranch: true,
  requireApprovalForPullRequest: true,
  requireApprovalForTaskMutation: true,
  defaultModelPolicy: '{"model":"default"}',
})
const title = computed(() =>
  props.template ? 'Edit workflow template' : 'Create workflow template',
)
watch(() => props.template, hydrate, { immediate: true })
function hydrate(template?: AiWorkflowTemplate | null) {
  form.name = template?.name ?? ''
  form.description = template?.description ?? ''
  form.workflowType = template?.workflowType ?? 'TASK_RESOLUTION'
  form.templateBody = template?.templateBody ?? ''
  form.allowedTools = parseTools(template?.allowedTools)
  form.approvalPolicy = template?.approvalPolicy ?? 'MANUAL'
  form.autoApproveReadOnly = template?.autoApproveReadOnly ?? true
  form.requireApprovalForComments = template?.requireApprovalForComments ?? true
  form.requireApprovalForBranch = template?.requireApprovalForBranch ?? true
  form.requireApprovalForPullRequest = template?.requireApprovalForPullRequest ?? true
  form.requireApprovalForTaskMutation = template?.requireApprovalForTaskMutation ?? true
  form.defaultModelPolicy = template?.defaultModelPolicy ?? '{"model":"default"}'
}
function parseTools(raw?: string | null): AiToolId[] {
  try {
    const parsed = JSON.parse(raw || '[]')
    return Array.isArray(parsed)
      ? parsed.filter((tool): tool is AiToolId =>
          AI_TOOL_OPTIONS.some((option) => option.value === tool),
        )
      : []
  } catch {
    return []
  }
}
function submit() {
  emit('save', {
    name: form.name.trim(),
    description: form.description.trim() || null,
    workflowType: form.workflowType,
    templateBody: form.templateBody.trim(),
    allowedTools: JSON.stringify(form.allowedTools),
    approvalPolicy: form.approvalPolicy,
    autoApproveReadOnly: form.autoApproveReadOnly,
    requireApprovalForComments: form.requireApprovalForComments,
    requireApprovalForBranch: form.requireApprovalForBranch,
    requireApprovalForPullRequest: form.requireApprovalForPullRequest,
    requireApprovalForTaskMutation: form.requireApprovalForTaskMutation,
    defaultModelPolicy: form.defaultModelPolicy.trim() || null,
  })
}
</script>
<template>
  <a-card :title="title" class="workflow-form-card">
    <a-form layout="vertical" @submit.prevent="submit">
      <a-form-item label="Name" required><a-input v-model:value="form.name" /></a-form-item>
      <a-form-item label="Workflow type"
        ><a-select v-model:value="form.workflowType"
          ><a-select-option value="TASK_RESOLUTION">Task resolution</a-select-option
          ><a-select-option value="BUG_TRIAGE">Bug triage</a-select-option
          ><a-select-option value="PR_REVIEW">PR review</a-select-option></a-select
        ></a-form-item
      >
      <a-form-item label="Description"
        ><a-textarea v-model:value="form.description" :rows="2"
      /></a-form-item>
      <a-form-item label="Template body" required
        ><a-textarea
          v-model:value="form.templateBody"
          :rows="6"
          placeholder="Describe the agent instructions, constraints, and expected output."
      /></a-form-item>
      <a-form-item label="Allowed tools"
        ><a-select
          v-model:value="form.allowedTools"
          mode="multiple"
          :options="AI_TOOL_OPTIONS"
          placeholder="Select tools the agent may use"
      /></a-form-item>
      <a-form-item label="Approval policy"
        ><a-radio-group v-model:value="form.approvalPolicy"
          ><a-radio value="AUTO">Auto</a-radio><a-radio value="MANUAL">Manual</a-radio
          ><a-radio value="ADMIN_ONLY">Admin only</a-radio></a-radio-group
        ></a-form-item
      >
      <a-space direction="vertical"
        ><a-checkbox v-model:checked="form.autoApproveReadOnly"
          >Auto-approve read-only work</a-checkbox
        ><a-checkbox v-model:checked="form.requireApprovalForComments"
          >Require approval for comments</a-checkbox
        ><a-checkbox v-model:checked="form.requireApprovalForBranch"
          >Require approval for branches</a-checkbox
        ><a-checkbox v-model:checked="form.requireApprovalForPullRequest"
          >Require approval for pull requests</a-checkbox
        ><a-checkbox v-model:checked="form.requireApprovalForTaskMutation"
          >Require approval for task updates</a-checkbox
        ></a-space
      >
      <a-form-item label="Default model policy JSON"
        ><a-textarea v-model:value="form.defaultModelPolicy" :rows="2"
      /></a-form-item>
      <a-space
        ><a-button type="primary" html-type="submit" :loading="saving">Save template</a-button
        ><a-button @click="emit('cancel')">Cancel</a-button></a-space
      >
    </a-form>
  </a-card>
</template>
<style scoped>
.workflow-form-card {
  border-radius: 16px;
}
</style>
