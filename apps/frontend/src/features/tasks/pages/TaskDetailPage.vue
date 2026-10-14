<script setup lang="ts">
import {
  computed,
  defineAsyncComponent,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import {
  ArrowLeftOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  ArrowUpOutlined,
  CalendarOutlined,
  ClockCircleOutlined,
  ThunderboltOutlined,
} from '@ant-design/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import TaskAttachmentsPanel from '../components/TaskAttachmentsPanel.vue'
import TaskEditorSaveBar from '../components/TaskEditorSaveBar.vue'
import TaskStatusChip from '../components/TaskStatusChip.vue'
import AiResolutionPanel from '../../aiResolution/components/AiResolutionPanel.vue'
import { useTasks } from '../composables/useTasks'
import { useTaskTypesStore } from '../stores/taskTypesStore'
import { TASK_STATUS_SELECT_OPTIONS } from '../constants/taskPresentation'
import {
  TASK_DESCRIPTION_MAX_LENGTH,
  TASK_DURATION_MAX_MINUTES,
  TASK_PRIORITY_MAX,
  TASK_PRIORITY_MIN,
  TASK_TITLE_MAX_LENGTH,
} from '../validation/taskFormValidation'
import type { EnergyLevel, Task, TaskLevel, TaskStatus, TaskType } from '../types'
import { findTaskTypeDefinition, isTaskTypeAllowedForLevel } from '../utils/taskTypeCompatibility'
import { taskHealth, taskHealthColor, taskHealthLabel } from '../utils/taskHealth'

const TaskDescriptionEditor = defineAsyncComponent(
  () => import('../components/TaskDescriptionEditor.vue'),
)

const ENERGY_LEVEL_OPTIONS: EnergyLevel[] = ['LOW', 'MEDIUM', 'HIGH']

const route = useRoute()
const router = useRouter()

const successMessage = ref('')
const taskNotFound = ref(false)
const savedSnapshot = ref('')
let loadRequest = 0
const { loading, saving, errorMessage, fetchTaskById, updateTaskDetails } = useTasks()
const taskTypesStore = useTaskTypesStore()
const taskTypeOptions = computed(() =>
  taskTypesStore.activeTaskTypes.filter((taskType) =>
    isTaskTypeAllowedForLevel(taskType, formState.taskLevel),
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
const healthTask = computed<Task>(() => ({
  id: formState.id,
  version: formState.version,
  projectId: formState.projectId,
  userId: '',
  taskKey: formState.taskKey,
  assigneeId: formState.assigneeId,
  parentTaskId: formState.parentTaskId,
  taskLevel: formState.taskLevel,
  taskType: formState.taskType,
  storyPoints: formState.storyPoints,
  releaseVersion: formState.releaseVersion,
  deletedAt: formState.deletedAt,
  title: formState.title,
  description: formState.description,
  status: formState.status,
  priority: formState.priority,
  dueAt: formState.dueAt || null,
  durationMinutes: formState.durationMinutes,
  energyLevel: formState.energyLevel ?? null,
  source: 'MANUAL',
  confidence: null,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
}))

function formSnapshot() {
  const { version: _version, ...fields } = formState
  return JSON.stringify(fields)
}
const hasChanges = computed(
  () => Boolean(savedSnapshot.value) && formSnapshot() !== savedSnapshot.value,
)
const canSave = computed(
  () =>
    Boolean(formState.id) &&
    hasChanges.value &&
    !loading.value &&
    !saving.value &&
    !taskNotFound.value,
)
const saveStatus = computed(() =>
  saving.value
    ? 'Saving…'
    : hasChanges.value
      ? 'Unsaved changes'
      : successMessage.value
        ? 'All changes saved'
        : 'No unsaved changes',
)
function confirmNavigation() {
  if (saving.value) return false
  return !hasChanges.value || window.confirm('You have unsaved changes. Leave without saving?')
}
onBeforeRouteLeave(confirmNavigation)
onBeforeRouteUpdate(confirmNavigation)
function beforeUnload(event: BeforeUnloadEvent) {
  if (!hasChanges.value && !saving.value) return
  event.preventDefault()
  event.returnValue = ''
}
function keyboardSave(event: KeyboardEvent) {
  if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
    event.preventDefault()
    if (canSave.value) void saveTask()
  }
}
onMounted(() => {
  void loadTask()
  window.addEventListener('beforeunload', beforeUnload)
  window.addEventListener('keydown', keyboardSave)
})
onBeforeUnmount(() => {
  loadRequest += 1
  window.removeEventListener('beforeunload', beforeUnload)
  window.removeEventListener('keydown', keyboardSave)
})

watch([taskId, routeProjectId], () => {
  void loadTask()
})

async function loadTask() {
  const request = ++loadRequest
  const requestedTaskId = taskId.value
  formState.id = ''
  savedSnapshot.value = ''
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
    const task = await fetchTaskById(requestedTaskId)
    if (request !== loadRequest) return

    if (!task) {
      taskNotFound.value = true
      return
    }

    hydrateForm(task)
    if (!taskNotFound.value) savedSnapshot.value = formSnapshot()
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
  if (saving.value || loading.value) return
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

    savedSnapshot.value = formSnapshot()
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

  if (formState.description.length > TASK_DESCRIPTION_MAX_LENGTH) {
    errorMessage.value = `Description must be at most ${TASK_DESCRIPTION_MAX_LENGTH} characters.`
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

  const taskType = findTaskTypeDefinition(taskTypesStore.activeTaskTypes, normalizedType)
  if (!taskType || !isTaskTypeAllowedForLevel(taskType, level)) {
    errorMessage.value = 'Task type is not valid for its hierarchy level.'
    return null
  }

  return taskType.key
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
  <AppLayout class="task-editor-layout">
    <template #title
      ><span class="editor-breadcrumb"
        >Tasks <span>/</span> {{ formState.taskKey || 'Task detail' }}</span
      ></template
    >
    <template #headerActions
      ><TaskEditorSaveBar
        v-if="formState.id"
        :status="saveStatus"
        :saving="saving"
        :can-save="canSave"
        :has-changes="hasChanges"
        @save="saveTask"
    /></template>
    <div class="task-detail-page">
      <div class="document-toolbar">
        <a-button
          type="text"
          class="back-button"
          @click="
            routeProjectId
              ? router.push({ name: 'project-detail', params: { id: routeProjectId } })
              : router.push('/tasks')
          "
        >
          <ArrowLeftOutlined /> {{ routeProjectId ? 'Back to project' : 'Back to tasks' }}
        </a-button>
        <TaskEditorSaveBar
          v-if="formState.id"
          class="mobile-save"
          :status="saveStatus"
          :saving="saving"
          :can-save="canSave"
          :has-changes="hasChanges"
          @save="saveTask"
        />
      </div>
      <a-alert
        v-if="errorMessage"
        type="error"
        show-icon
        :message="errorMessage"
        class="editor-alert"
      />
      <div v-if="loading" class="document-loading"><a-spin tip="Loading task..." /></div>
      <a-empty
        v-else-if="isEmptyState"
        description="No task found for this ID. It may have been removed or belongs to another user."
      />
      <div v-else-if="!formState.id" class="document-loading">
        <a-button v-if="isValidId" @click="loadTask">Retry loading task</a-button>
      </div>
      <template v-else>
        <a-form layout="vertical" class="document-form" @submit.prevent="saveTask">
          <a-textarea
            v-model:value="formState.title"
            class="document-title"
            aria-label="Task title"
            placeholder="Untitled task"
            :auto-size="{ minRows: 1 }"
            :disabled="saving"
            :maxlength="TASK_TITLE_MAX_LENGTH"
          />
          <div class="task-properties">
            <div class="property-row">
              <label for="task-status"><CheckCircleOutlined aria-hidden="true" />Status</label
              ><a-select id="task-status" v-model:value="formState.status" :disabled="saving"
                ><a-select-option
                  v-for="option in TASK_STATUS_SELECT_OPTIONS"
                  :key="option.value"
                  :value="option.value"
                  ><TaskStatusChip :status="option.value" /></a-select-option
              ></a-select>
            </div>
            <div class="property-row">
              <label for="task-due"><CalendarOutlined aria-hidden="true" />Due date</label
              ><a-input
                id="task-due"
                v-model:value="formState.dueAt"
                type="datetime-local"
                :disabled="saving"
              />
            </div>
            <div class="property-row">
              <label for="task-priority"><ArrowUpOutlined aria-hidden="true" />Priority</label
              ><a-select id="task-priority" v-model:value="formState.priority" :disabled="saving"
                ><a-select-option :value="1">Urgent</a-select-option
                ><a-select-option :value="2">High</a-select-option
                ><a-select-option :value="3">Medium</a-select-option
                ><a-select-option :value="4">Low</a-select-option></a-select
              >
            </div>
            <div class="property-row">
              <label for="task-duration"><ClockCircleOutlined aria-hidden="true" />Duration</label
              ><a-input-number
                id="task-duration"
                v-model:value="formState.durationMinutes"
                :min="1"
                :max="TASK_DURATION_MAX_MINUTES"
                :step="5"
                placeholder="Empty"
                addon-after="min"
                :disabled="saving"
              />
            </div>
            <div class="property-row">
              <label for="task-type"><FileTextOutlined aria-hidden="true" />Type</label
              ><a-select
                id="task-type"
                v-model:value="formState.taskType"
                :loading="taskTypesStore.loading"
                placeholder="Select type"
                :disabled="saving"
                ><a-select-option
                  v-for="taskType in taskTypeOptions"
                  :key="taskType.id"
                  :value="taskType.key"
                  >{{ taskType.name }}</a-select-option
                ></a-select
              >
            </div>
            <div class="property-row">
              <label for="task-energy"><ThunderboltOutlined aria-hidden="true" />Energy</label
              ><a-select
                id="task-energy"
                v-model:value="formState.energyLevel"
                allow-clear
                placeholder="Empty"
                :disabled="saving"
                ><a-select-option
                  v-for="energy in ENERGY_LEVEL_OPTIONS"
                  :key="energy"
                  :value="energy"
                  >{{ energy.charAt(0) + energy.slice(1).toLowerCase() }}</a-select-option
                ></a-select
              >
            </div>
          </div>
          <div class="document-body">
            <TaskDescriptionEditor v-model="formState.description" :disabled="saving" />
          </div>
        </a-form>
        <div class="document-extras">
          <details class="document-section">
            <summary><FileTextOutlined /> Task metadata</summary>
            <a-descriptions bordered :column="{ xs: 1, sm: 2 }" size="small">
              <a-descriptions-item label="ID">{{ formState.id }}</a-descriptions-item>
              <a-descriptions-item label="Health"
                ><a-tag :color="taskHealthColor(taskHealth(healthTask))">{{
                  taskHealthLabel(taskHealth(healthTask))
                }}</a-tag></a-descriptions-item
              >
              <a-descriptions-item label="Task key">{{
                formState.taskKey || '—'
              }}</a-descriptions-item>
              <a-descriptions-item label="Level">{{
                formState.taskLevel || '—'
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
              <a-descriptions-item label="Parent task">{{
                formState.parentTaskId || '—'
              }}</a-descriptions-item>
              <a-descriptions-item v-if="formState.deletedAt" label="Deleted at">{{
                formState.deletedAt
              }}</a-descriptions-item>
            </a-descriptions>
          </details>
          <details class="document-section">
            <summary>Attachments</summary>
            <TaskAttachmentsPanel :task-id="formState.id" />
          </details>
          <details v-if="formState.projectId" class="document-section">
            <summary>AI assistance</summary>
            <AiResolutionPanel :task-id="formState.id" :project-id="formState.projectId" />
          </details>
        </div>
      </template>
    </div>
  </AppLayout>
</template>

<style scoped>
.task-editor-layout {
  background: var(--tm-surface);
}
.task-editor-layout :deep(.desktop-topbar) {
  min-height: 56px;
  padding-top: 12px;
  padding-bottom: 12px;
}
.document-toolbar .mobile-save {
  display: none;
}
.task-editor-layout :deep(.workspace-main) {
  background: var(--tm-surface);
}
.task-editor-layout :deep(.content) {
  padding: 18px 48px 64px;
}
.editor-breadcrumb {
  font-size: 14px;
  font-weight: 400;
  color: var(--tm-text-muted);
}
.editor-breadcrumb span {
  margin: 0 10px;
  color: var(--tm-border);
}
.task-detail-page {
  --tm-primary: #2563eb;
  --tm-primary-hover: #1d4ed8;
  width: 100%;
  max-width: 928px;
  margin: 0 auto;
}
.document-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}
.back-button {
  padding-left: 0;
  color: var(--tm-text-muted);
  font-size: 13px;
}
.editor-alert {
  margin-bottom: 24px;
}
.document-loading {
  display: grid;
  place-items: center;
  min-height: 240px;
}
.document-title {
  padding: 0 0 14px;
  border: 0;
  border-radius: 0;
  box-shadow: none !important;
  background: transparent;
  color: var(--tm-text);
  font-size: 40px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: -0.035em;
  resize: none;
  transition: none;
}
.document-title:focus-visible {
  outline: 2px solid var(--tm-primary);
  outline-offset: 6px;
}
.task-properties {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px 36px;
  margin: 10px 0 28px;
}
.property-row {
  display: grid;
  grid-template-columns: 140px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.property-row label {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--tm-text-muted);
  font-size: 14px;
}
.property-row label :deep(.anticon) {
  width: 20px;
  font-size: 17px;
}
.property-row :deep(.ant-select-selector),
.property-row :deep(.ant-input),
.property-row :deep(.ant-input-number) {
  min-height: 38px;
}
.property-row :deep(.ant-select-single .ant-select-selector .ant-select-selection-item),
.property-row :deep(.ant-select-single .ant-select-selector .ant-select-selection-placeholder) {
  line-height: 36px;
}
.property-row :deep(.ant-input-number-input) {
  height: 36px;
}
.property-row :deep(.ant-select),
.property-row :deep(.ant-input-number-group-wrapper) {
  width: 100%;
  min-width: 0;
}
.property-row :deep(.ant-select-selector),
.property-row :deep(.ant-input),
.property-row :deep(.ant-input-number),
.property-row :deep(.ant-input-number-group-addon) {
  border-color: var(--tm-border);
  border-radius: 5px;
  font-size: 14px;
  box-shadow: none;
  background: var(--tm-surface);
  color: var(--tm-text);
}
.property-row :deep(.ant-input-number-input) {
  color: var(--tm-text);
}
.property-row :deep(.ant-select-arrow),
.property-row :deep(.ant-select-selection-placeholder) {
  color: var(--tm-text-muted);
}
:global([data-theme='dark']) .property-row :deep(input[type='datetime-local']) {
  color-scheme: dark;
}
.document-body {
  border-top: 1px solid var(--tm-border);
  padding-top: 4px;
}
.document-extras {
  margin-top: 24px;
}
.document-section {
  border-top: 1px solid var(--tm-border);
  padding: 0;
}
.document-section:last-child {
  border-bottom: 1px solid var(--tm-border);
}
.document-section summary {
  padding: 13px 4px;
  cursor: pointer;
  font-size: 14px;
  color: var(--tm-text);
}
.document-section summary:focus-visible {
  outline: 2px solid var(--tm-primary);
  outline-offset: -2px;
}
.document-section[open] {
  padding-bottom: 20px;
}
.document-section :deep(.ant-descriptions-item-content) {
  overflow-wrap: anywhere;
}
@media (max-width: 1200px) {
  .document-toolbar .mobile-save {
    display: flex;
    margin-left: auto;
  }
}
@media (max-width: 760px) {
  .task-editor-layout :deep(.content) {
    padding: 20px 28px 40px;
  }
  .document-title {
    font-size: 32px;
  }
  .task-properties {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .document-toolbar {
    flex-wrap: wrap;
    margin-bottom: 24px;
  }
}
@media (max-width: 420px) {
  .document-title {
    font-size: 28px;
  }
  .document-toolbar .mobile-save {
    width: 100%;
    justify-content: space-between;
  }
  .property-row {
    grid-template-columns: 100px minmax(0, 1fr);
  }
}
</style>
