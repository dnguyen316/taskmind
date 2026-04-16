<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTasks } from '../composables/useTasks'
import { TASK_STATUS_OPTIONS } from '../constants/taskConstants'

const ENERGY_LEVEL_OPTIONS = ['LOW', 'MEDIUM', 'HIGH']

const route = useRoute()
const router = useRouter()

const successMessage = ref('')
const taskNotFound = ref(false)
const { loading, saving, errorMessage, fetchTaskById, updateTaskDetails } = useTasks()

const formState = reactive({
  id: '',
  title: '',
  description: '',
  priority: 3,
  dueAt: '',
  durationMinutes: null,
  energyLevel: undefined,
  status: 'TODO',
})

const taskId = computed(() => String(route.params.id ?? '').trim())
const isValidId = computed(() => /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(taskId.value))
const isEmptyState = computed(() => !loading.value && !errorMessage.value && taskNotFound.value)

onMounted(() => {
  void loadTask()
})

watch(taskId, () => {
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
  } catch {
    // errorMessage is managed by useTasks
  }
}

function hydrateForm(task) {
  formState.id = task.id
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

  try {
    const updated = await updateTaskDetails(formState.id, {
      title: formState.title.trim(),
      description: formState.description.trim() || null,
      priority: Number(formState.priority),
      dueAt: formState.dueAt ? new Date(formState.dueAt).toISOString() : null,
      durationMinutes: formState.durationMinutes ? Number(formState.durationMinutes) : null,
      energyLevel: formState.energyLevel || null,
      status: formState.status,
    })

    if (updated && typeof updated === 'object') {
      hydrateForm(updated)
    }

    successMessage.value = 'Task details saved.'
  } catch {
    // errorMessage is managed by useTasks
  }
}

function toDatetimeLocal(value) {
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
  <main class="task-detail-page">
    <a-card title="Task detail" class="surface-card">
      <a-space direction="vertical" style="width: 100%" size="middle">
        <a-space>
          <a-button @click="router.push('/tasks')">Back to tasks</a-button>
          <span class="task-id" v-if="formState.id">ID: {{ formState.id }}</span>
        </a-space>

        <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />
        <a-alert v-else-if="successMessage" type="success" show-icon :message="successMessage" />

        <a-spin v-if="loading" tip="Loading task..." />

        <a-empty
          v-else-if="isEmptyState"
          description="No task found for this ID. It may have been removed or belongs to another user."
        />

        <a-form v-else layout="vertical" @submit.prevent="saveTask">
          <a-row :gutter="12">
            <a-col :xs="24" :md="12">
              <a-form-item label="Title" required>
                <a-input v-model:value="formState.title" placeholder="Task title" />
              </a-form-item>
            </a-col>

            <a-col :xs="24" :md="12">
              <a-form-item label="Status">
                <a-select v-model:value="formState.status">
                  <a-select-option v-for="status in TASK_STATUS_OPTIONS" :key="status" :value="status">
                    {{ status }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="Description">
            <a-textarea v-model:value="formState.description" :rows="3" placeholder="Optional context" />
          </a-form-item>

          <a-row :gutter="12">
            <a-col :xs="24" :md="6">
              <a-form-item label="Priority (1 highest)">
                <a-input-number v-model:value="formState.priority" :min="1" :max="4" :step="1" style="width: 100%" />
              </a-form-item>
            </a-col>

            <a-col :xs="24" :md="6">
              <a-form-item label="Due at">
                <a-input v-model:value="formState.dueAt" type="datetime-local" />
              </a-form-item>
            </a-col>

            <a-col :xs="24" :md="6">
              <a-form-item label="Duration (minutes)">
                <a-input-number v-model:value="formState.durationMinutes" :min="1" :step="5" style="width: 100%" />
              </a-form-item>
            </a-col>

            <a-col :xs="24" :md="6">
              <a-form-item label="Energy level">
                <a-select v-model:value="formState.energyLevel" allow-clear placeholder="Select energy level">
                  <a-select-option v-for="energy in ENERGY_LEVEL_OPTIONS" :key="energy" :value="energy">
                    {{ energy }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-button type="primary" html-type="submit" :loading="saving" :disabled="loading || isEmptyState">
            Save changes
          </a-button>
        </a-form>
      </a-space>
    </a-card>
  </main>
</template>

<style scoped>
.task-detail-page {
  min-height: 100vh;
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 20px 40px;
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
