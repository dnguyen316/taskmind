<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useProjects } from '../../projects/composables/useProjects'
import { useCapture } from '../composables/useCapture'
import type { CapturedTaskDraft } from '../composables/types'

interface DraftReviewState {
  projectId: string
  description: string
  dueAt: string
  assigneeId: string
  parentTaskId: string
  status: string
  error: string
  accepting: boolean
  rejecting: boolean
}

const text = ref('')
const draftStates = reactive<Record<string, DraftReviewState>>({})
const rejectReason = ref('Not useful')
const { loading, result, capture, acceptDraft, rejectDraft } = useCapture()
const { projects, loading: loadingProjects, fetchProjects } = useProjects()

const activeProjects = computed(() => projects.value.filter((project) => !project.archivedAt))
const hasProjectOptions = computed(() => activeProjects.value.length > 0)

onMounted(() => {
  void fetchProjects()
})

watch(activeProjects, (nextProjects) => {
  if (nextProjects.length !== 1) {
    return
  }

  for (const state of Object.values(draftStates)) {
    if (!state.projectId) {
      state.projectId = nextProjects[0].id
    }
  }
})

function draftKey(draft: CapturedTaskDraft, index: number) {
  return `${index}:${draft.title}`
}

function defaultProjectId() {
  return activeProjects.value.length === 1 ? activeProjects.value[0].id : ''
}

function reviewState(draft: CapturedTaskDraft, index: number) {
  const key = draftKey(draft, index)

  if (!draftStates[key]) {
    draftStates[key] = {
      projectId: defaultProjectId(),
      description: '',
      dueAt: '',
      assigneeId: '',
      parentTaskId: '',
      status: '',
      error: '',
      accepting: false,
      rejecting: false,
    }
  }

  return draftStates[key]
}

function acceptDisabled(draft: CapturedTaskDraft, index: number) {
  const state = reviewState(draft, index)

  return state.accepting || state.rejecting || !draft.title.trim() || !state.projectId.trim()
}

function optionalField(value: string) {
  return value.trim() || null
}

function dueAtValue(value: string) {
  return value ? new Date(value).toISOString() : null
}

async function accept(draft: CapturedTaskDraft, index: number) {
  const state = reviewState(draft, index)
  state.error = ''
  state.status = ''

  if (acceptDisabled(draft, index)) {
    state.error = hasProjectOptions.value
      ? 'Select a project before accepting.'
      : 'Create a project before accepting drafts.'
    return
  }

  state.accepting = true
  try {
    const response = await acceptDraft({
      draft,
      projectId: state.projectId,
      assigneeId: optionalField(state.assigneeId),
      parentTaskId: optionalField(state.parentTaskId),
      description: optionalField(state.description),
      dueAt: dueAtValue(state.dueAt),
    })
    state.status = `Accepted as task ${response.taskId}`
  } catch (error: unknown) {
    state.error = error instanceof Error ? error.message : 'Failed to accept draft.'
  } finally {
    state.accepting = false
  }
}

async function reject(draft: CapturedTaskDraft, index: number) {
  const state = reviewState(draft, index)
  state.error = ''
  state.status = ''
  state.rejecting = true

  try {
    await rejectDraft({ draft, reason: rejectReason.value })
    state.status = 'Rejected'
  } catch (error: unknown) {
    state.error = error instanceof Error ? error.message : 'Failed to reject draft.'
  } finally {
    state.rejecting = false
  }
}
</script>

<template>
  <a-card title="AI capture">
    <a-space direction="vertical" style="width: 100%" size="middle">
      <a-textarea v-model:value="text" :rows="4" placeholder="Paste messy notes or action items" />
      <a-button type="primary" :loading="loading" :disabled="!text.trim()" @click="capture(text)">
        Draft tasks
      </a-button>
    </a-space>

    <a-list v-if="result" :data-source="result.drafts" bordered class="capture-drafts">
      <template #renderItem="{ item, index }">
        <a-list-item>
          <a-list-item-meta>
            <template #title>{{ item.title }}</template>
            <template #description>
              <a-space direction="vertical" style="width: 100%" size="small">
                <span>
                  {{ item.durationMinutes }} min · priority {{ item.priority }} · confidence
                  {{ Math.round(item.confidence * 100) }}%
                </span>

                <a-row :gutter="12">
                  <a-col :xs="24" :md="12">
                    <a-form-item label="Project" required>
                      <a-select
                        v-model:value="reviewState(item, index).projectId"
                        placeholder="Select a project"
                        :loading="loadingProjects"
                        :disabled="!hasProjectOptions"
                      >
                        <a-select-option
                          v-for="project in activeProjects"
                          :key="project.id"
                          :value="project.id"
                        >
                          {{ project.name }} ({{ project.key }})
                        </a-select-option>
                      </a-select>
                    </a-form-item>
                  </a-col>

                  <a-col :xs="24" :md="12">
                    <a-form-item label="Due date">
                      <a-date-picker
                        v-model:value="reviewState(item, index).dueAt"
                        show-time
                        style="width: 100%"
                        format="YYYY-MM-DD HH:mm"
                        value-format="YYYY-MM-DDTHH:mm"
                        placeholder="Set due date"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-form-item label="Description">
                  <a-textarea
                    v-model:value="reviewState(item, index).description"
                    :rows="2"
                    placeholder="Optional task details to save with the accepted draft"
                  />
                </a-form-item>

                <a-row :gutter="12">
                  <a-col :xs="24" :md="12">
                    <a-form-item label="Assignee ID (optional)">
                      <a-input
                        v-model:value="reviewState(item, index).assigneeId"
                        placeholder="User UUID"
                      />
                    </a-form-item>
                  </a-col>

                  <a-col :xs="24" :md="12">
                    <a-form-item label="Parent task ID (optional)">
                      <a-input
                        v-model:value="reviewState(item, index).parentTaskId"
                        placeholder="Task UUID"
                      />
                    </a-form-item>
                  </a-col>
                </a-row>

                <a-alert
                  v-if="reviewState(item, index).error"
                  type="error"
                  show-icon
                  :message="reviewState(item, index).error"
                />
                <a-alert
                  v-if="reviewState(item, index).status"
                  type="success"
                  show-icon
                  :message="reviewState(item, index).status"
                />

                <a-space>
                  <a-button
                    size="small"
                    type="primary"
                    :loading="reviewState(item, index).accepting"
                    :disabled="acceptDisabled(item, index)"
                    @click="accept(item, index)"
                  >
                    Accept
                  </a-button>
                  <a-button
                    size="small"
                    danger
                    :loading="reviewState(item, index).rejecting"
                    :disabled="reviewState(item, index).accepting"
                    @click="reject(item, index)"
                  >
                    Reject
                  </a-button>
                </a-space>
              </a-space>
            </template>
          </a-list-item-meta>
        </a-list-item>
      </template>
    </a-list>
  </a-card>
</template>

<style scoped>
.capture-drafts {
  margin-top: 16px;
}
</style>
