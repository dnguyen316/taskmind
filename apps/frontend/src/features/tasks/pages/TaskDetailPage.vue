<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppLayout from '../components/AppLayout.vue'
import TaskAttachmentsPanel from '../components/TaskAttachmentsPanel.vue'
import TaskDescriptionEditor from '../components/TaskDescriptionEditor.vue'
import TaskStatusChip from '../components/TaskStatusChip.vue'
import AiResolutionPanel from '../../aiResolution/components/AiResolutionPanel.vue'
import { useTasks } from '../composables/useTasks'
import { useTaskTypesStore } from '../stores/taskTypesStore'
import { TASK_STATUS_SELECT_OPTIONS } from '../constants/taskPresentation'
import {
  TASK_DURATION_MAX_MINUTES,
  TASK_PRIORITY_MAX,
  TASK_PRIORITY_MIN,
  TASK_TITLE_MAX_LENGTH,
} from '../validation/taskFormValidation'
import type { EnergyLevel, Task, TaskLevel, TaskStatus, TaskType } from '../types'

const ENERGY_LEVEL_OPTIONS: EnergyLevel[] = ['LOW', 'MEDIUM', 'HIGH']

const route = useRoute()
const router = useRouter()

const successMessage = ref('')
const taskNotFound = ref(false)
const { loading, saving, errorMessage, fetchTaskById, updateTaskDetails } = useTasks()
const taskTypesStore = useTaskTypesStore()
const taskTypeOptions = computed(() =>
  taskTypesStore.activeTaskTypes.filter((taskType) =>
    isTaskTypeCompatibleWithLevel(taskType.key, formState.taskLevel),
  ),
)

interface TaskDetailFormState {
  id: string
  version: number | null
  projectId: string
  taskKey: string | null
  assigneeId: string | null
  parentTaskId: string | null
  taskLevel: TaskLevel | null
  taskType: TaskType | null
  storyPoints: number | null
  releaseVersion: string | null
  deletedAt: string | null
  title: string
  description: string
  priority: number
  dueAt: string
  durationMinutes: number | null
  energyLevel: EnergyLevel | undefined
  status: TaskStatus
}

const formState = reactive<TaskDetailFormState>({
  id: '',
  version: null,
  projectId: '',
  taskKey: null,
  assigneeId: null,
  parentTaskId: null,
  taskLevel: null,
  taskType: null,
  storyPoints: null,
  releaseVersion: null,
  deletedAt: null,
  title: '',
  description: '',
  priority: 3,
  dueAt: '',
  durationMinutes: null,
  energyLevel: undefined,
  status: 'TODO',
})

const taskId = computed(() => String(route.params.taskId ?? route.params.id ?? '').trim())
const routeProjectId = computed(() => String(route.params.projectId ?? '').trim())
const isValidId = computed(() =>
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(taskId.value),
)
const isEmptyState = computed(() => !loading.value && !errorMessage.value && taskNotFound.value)

onMounted(() => {
  void loadTask()
})

watch([taskId, routeProjectId], () => {
  void loadTask()
})

async function loadTask() {
  successMessage.value = ''
  errorMessage.value = ''
  taskNotFound.value = false

  if (!taskId.value) {
    loading.value = false
    errorMessage.value = 'Missing task id in the route.'
    return
  }

  if (!isValidId.value) {
    loading.value = false
    errorMessage.value = 'Task id is invalid. Please use a valid UUID.'
    return
  }

  try {
    const task = await fetchTaskById(taskId.value)

    if (!task) {
      taskNotFound.value = true
      return
    }

    hydrateForm(task)
    void taskTypesStore.fetchTaskTypes(task.projectId)
  } catch {
    // errorMessage is managed by useTasks
  }
}

function hydrateForm(task: Task) {
  const taskProjectId = task.projectId ?? ''

  if (routeProjectId.value && taskProjectId && taskProjectId !== routeProjectId.value) {
    errorMessage.value = 'This task does not belong to the project in the current route.'
    taskNotFound.value = true
    return
  }

  formState.id = task.id
  formState.version = task.version ?? null
  formState.projectId = taskProjectId || routeProjectId.value
  formState.taskKey = task.taskKey
  formState.assigneeId = task.assigneeId
  formState.parentTaskId = task.parentTaskId
  formState.taskLevel = task.taskLevel
  formState.taskType = task.taskType
  formState.storyPoints = task.storyPoints
  formState.releaseVersion = task.releaseVersion
  formState.deletedAt = task.deletedAt
  formState.title = task.title ?? ''
  formState.description = task.description ?? ''
  formState.priority = Number(task.priority ?? 3)
  formState.dueAt = toDatetimeLocal(task.dueAt)
  formState.durationMinutes = task.durationMinutes ?? null
  formState.energyLevel = task.energyLevel ?? undefined
  formState.status = task.status ?? 'TODO'
}

async function saveTask() {
  if (!formState.id) {
    errorMessage.value = 'Cannot save because the task was not loaded.'
    return
  }

  successMessage.value = ''
  errorMessage.value = ''

  const payload = validateTaskDetailForm()
  if (!payload) {
    return
  }

  try {
    const updated = await updateTaskDetails(formState.id, payload)

    if (updated && typeof updated === 'object') {
      hydrateForm(updated)
    }

    successMessage.value = 'Task details saved.'
  } catch {
    // errorMessage is managed by useTasks
  }
}

function validateTaskDetailForm() {
  const title = formState.title.trim()
  if (!title) {
    errorMessage.value = 'Title is required.'
    return null
  }

  if (title.length > TASK_TITLE_MAX_LENGTH) {
    errorMessage.value = `Title must be at most ${TASK_TITLE_MAX_LENGTH} characters.`
    return null
  }

  const priority = Number(formState.priority)
  if (!Number.isInteger(priority) || priority < TASK_PRIORITY_MIN || priority > TASK_PRIORITY_MAX) {
    errorMessage.value = `Priority must be an integer between ${TASK_PRIORITY_MIN} and ${TASK_PRIORITY_MAX}.`
    return null
  }

  const normalizedTaskType = normalizeTaskType(formState.taskType, formState.taskLevel)
  if (!normalizedTaskType) {
    return null
  }

  const durationMinutes = normalizeDurationMinutes(formState.durationMinutes)
  if (durationMinutes === undefined) {
    return null
  }

  const dueAt = normalizeDueAt(formState.dueAt)
  if (dueAt === undefined) {
    return null
  }

  return {
    version: formState.version,
    projectId: formState.projectId.trim(),
    title,
    description: formState.description.trim() || null,
    priority,
    dueAt,
    durationMinutes,
    energyLevel: formState.energyLevel || null,
    status: formState.status,
    taskType: normalizedTaskType,
  }
}

function normalizeTaskType(type: TaskType | null, level: TaskLevel | null): TaskType | null {
  const normalizedType = type?.trim().toUpperCase() || null
  if (!normalizedType) {
    errorMessage.value = 'Task type is required.'
    return null
  }

  if (!isTaskTypeCompatibleWithLevel(normalizedType, level)) {
    errorMessage.value = 'Task type is not valid for its hierarchy level.'
    return null
  }

  return normalizedType
}

function isTaskTypeCompatibleWithLevel(
  type: string | null | undefined,
  level: TaskLevel | null,
): boolean {
  if (!type || !level) {
    return false
  }

  const normalizedType = type.trim().toUpperCase()
  return (
    (normalizedType !== 'EPIC' || level === 'EPIC') &&
    (normalizedType !== 'STORY' || level === 'STORY') &&
    (normalizedType !== 'SUBTASK' || level === 'SUBTASK') &&
    (normalizedType !== 'MILESTONE' || level !== 'SUBTASK')
  )
}

function normalizeDurationMinutes(value: number | null): number | null | undefined {
  if (value === null || value === 0) {
    return null
  }

  const durationMinutes = Number(value)
  if (!Number.isInteger(durationMinutes) || durationMinutes < 0) {
    errorMessage.value = 'Duration must be a whole number of minutes, or blank.'
    return undefined
  }

  if (durationMinutes === 0) {
    return null
  }

  if (durationMinutes > TASK_DURATION_MAX_MINUTES) {
    errorMessage.value = `Duration must be at most ${TASK_DURATION_MAX_MINUTES} minutes.`
    return undefined
  }

  return durationMinutes
}

function normalizeDueAt(value: string): string | null | undefined {
  const dueAtValue = value.trim()
  if (!dueAtValue) {
    return null
  }

  const dueAt = new Date(dueAtValue)
  if (Number.isNaN(dueAt.getTime())) {
    errorMessage.value = 'Due date must be a valid date and time.'
    return undefined
  }

  return dueAt.toISOString()
}

function toDatetimeLocal(value: string | null) {
  if (!value) {
    return ''
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }

  const offsetMs = date.getTimezoneOffset() * 60 * 1000
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16)
}
</script>

<template>
  <AppLayout>
    <div class="task-detail-page">
      <a-card title="Task detail" class="surface-card">
        <a-space direction="vertical" style="width: 100%" size="middle">
          <a-space>
            <a-button
              @click="
                routeProjectId
                  ? router.push({ name: 'project-detail', params: { id: routeProjectId } })
                  : router.push('/tasks')
              "
            >
              {{ routeProjectId ? 'Back to project' : 'Back to tasks' }}
            </a-button>
            <span class="task-id" v-if="formState.id">
              {{ formState.taskKey ? `Key: ${formState.taskKey}` : `ID: ${formState.id}` }}
            </span>
          </a-space>

          <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />
          <a-alert v-else-if="successMessage" type="success" show-icon :message="successMessage" />

          <a-spin v-if="loading" tip="Loading task..." />

          <a-empty
            v-else-if="isEmptyState"
            description="No task found for this ID. It may have been removed or belongs to another user."
          />

          <template v-else>
            <a-descriptions bordered :column="2" size="small" title="Task metadata">
              <a-descriptions-item label="ID">{{ formState.id }}</a-descriptions-item>
              <a-descriptions-item label="Task key">{{
                formState.taskKey || '—'
              }}</a-descriptions-item>
              <a-descriptions-item label="Level">{{
                formState.taskLevel || '—'
              }}</a-descriptions-item>
              <a-descriptions-item label="Type">{{
                formState.taskType || '—'
              }}</a-descriptions-item>
              <a-descriptions-item label="Story points">{{
                formState.storyPoints ?? '—'
              }}</a-descriptions-item>
              <a-descriptions-item label="Release">{{
                formState.releaseVersion || '—'
              }}</a-descriptions-item>
              <a-descriptions-item label="Assignee">{{
                formState.assigneeId || '—'
              }}</a-descriptions-item>
              <a-descriptions-item label="Status">
                <TaskStatusChip :status="formState.status" />
              </a-descriptions-item>
              <a-descriptions-item label="Parent task">{{
                formState.parentTaskId || '—'
              }}</a-descriptions-item>
              <a-descriptions-item v-if="formState.deletedAt" label="Deleted at">
                {{ formState.deletedAt }}
              </a-descriptions-item>
            </a-descriptions>

            <a-form layout="vertical" @submit.prevent="saveTask">
              <a-row :gutter="12">
                <a-col :xs="24" :md="12">
                  <a-form-item label="Title" required>
                    <a-input v-model:value="formState.title" placeholder="Task title" />
                  </a-form-item>
                </a-col>

                <a-col :xs="24" :md="12">
                  <a-form-item label="Type" required>
                    <a-select
                      v-model:value="formState.taskType"
                      :loading="taskTypesStore.loading"
                      placeholder="Select task type"
                    >
                      <a-select-option
                        v-for="taskType in taskTypeOptions"
                        :key="taskType.id"
                        :value="taskType.key"
                      >
                        {{ taskType.name }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>

                <a-col :xs="24" :md="12">
                  <a-form-item label="Status">
                    <a-select v-model:value="formState.status">
                      <a-select-option
                        v-for="option in TASK_STATUS_SELECT_OPTIONS"
                        :key="option.value"
                        :value="option.value"
                      >
                        <TaskStatusChip :status="option.value" />
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>

              <a-form-item label="Description">
                <TaskDescriptionEditor v-model="formState.description" />
              </a-form-item>

              <a-row :gutter="12">
                <a-col :xs="24" :md="6">
                  <a-form-item label="Priority (1 highest)">
                    <a-input-number
                      v-model:value="formState.priority"
                      :min="1"
                      :max="4"
                      :step="1"
                      style="width: 100%"
                    />
                  </a-form-item>
                </a-col>

                <a-col :xs="24" :md="6">
                  <a-form-item label="Due at">
                    <a-input v-model:value="formState.dueAt" type="datetime-local" />
                  </a-form-item>
                </a-col>

                <a-col :xs="24" :md="6">
                  <a-form-item label="Duration (minutes)">
                    <a-input-number
                      v-model:value="formState.durationMinutes"
                      :min="1"
                      :step="5"
                      style="width: 100%"
                    />
                  </a-form-item>
                </a-col>

                <a-col :xs="24" :md="6">
                  <a-form-item label="Energy level">
                    <a-select
                      v-model:value="formState.energyLevel"
                      allow-clear
                      placeholder="Select energy level"
                    >
                      <a-select-option
                        v-for="energy in ENERGY_LEVEL_OPTIONS"
                        :key="energy"
                        :value="energy"
                      >
                        {{ energy }}
                      </a-select-option>
                    </a-select>
                  </a-form-item>
                </a-col>
              </a-row>

              <a-button
                type="primary"
                html-type="submit"
                :loading="saving"
                :disabled="loading || isEmptyState"
              >
                Save changes
              </a-button>
            </a-form>

            <AiResolutionPanel
              v-if="formState.id && formState.projectId"
              :task-id="formState.id"
              :project-id="formState.projectId"
            />

            <TaskAttachmentsPanel v-if="formState.id" :task-id="formState.id" />
          </template>
        </a-space>
      </a-card>
    </div>
  </AppLayout>
</template>

<style scoped>
.task-detail-page {
  width: 100%;
  max-width: 1100px;
  margin: 0 auto;
}

.surface-card {
  border-radius: 18px;
}

.task-id {
  margin: 0;
  font-weight: 600;
  color: #64748b;
}
</style>
