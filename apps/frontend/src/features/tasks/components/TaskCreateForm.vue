<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Form as VeeForm, Field, ErrorMessage } from 'vee-validate'
import type { FormState, SubmissionContext } from 'vee-validate'
import * as yup from 'yup'
import { DEFAULT_CREATE_TASK_FORM } from '../constants/taskConstants'
import {
  TASK_DESCRIPTION_MAX_LENGTH,
  TASK_DURATION_MAX_MINUTES,
  TASK_DURATION_MIN_MINUTES,
  TASK_PRIORITY_MAX,
  TASK_PRIORITY_MIN,
  TASK_TITLE_MAX_LENGTH,
} from '../validation/taskFormValidation'
import type { Project } from '../../projects/types'
import type { CreateTaskFormValues, CreateTaskPayload } from '../types'

type SubmitTaskPayload = Omit<CreateTaskPayload, 'userId' | 'source'>

interface TaskCreateFormProps {
  saving?: boolean
  onSubmitTask: (payload: SubmitTaskPayload) => Promise<void> | void
  projectOptions?: Project[]
  defaultProjectId?: string
}

const props = withDefaults(defineProps<TaskCreateFormProps>(), {
  saving: false,
  projectOptions: () => [],
  defaultProjectId: '',
})

const schema = yup.object({
  projectId: yup.string().trim().required('Project selection is required'),
  title: yup
    .string()
    .trim()
    .required('Title is required')
    .max(TASK_TITLE_MAX_LENGTH, `Title must be at most ${TASK_TITLE_MAX_LENGTH} characters`),
  description: yup
    .string()
    .max(
      TASK_DESCRIPTION_MAX_LENGTH,
      `Description must be at most ${TASK_DESCRIPTION_MAX_LENGTH} characters`,
    )
    .nullable(),
  priority: yup.number().integer().min(TASK_PRIORITY_MIN).max(TASK_PRIORITY_MAX).required(),
  durationMinutes: yup
    .number()
    .transform((value, originalValue) =>
      originalValue === '' || originalValue === null ? null : value,
    )
    .integer()
    .min(TASK_DURATION_MIN_MINUTES)
    .max(TASK_DURATION_MAX_MINUTES)
    .nullable()
    .optional(),
  dueAt: yup.string().nullable(),
  status: yup.string().oneOf(['TODO', 'IN_PROGRESS', 'DONE', 'ARCHIVED']).required(),
})

const initialValues = computed<CreateTaskFormValues>(() => ({
  ...DEFAULT_CREATE_TASK_FORM,
  projectId: props.defaultProjectId || DEFAULT_CREATE_TASK_FORM.projectId,
}))

const hasProjectOptions = computed(() => props.projectOptions.length > 0)
const taskForm = ref<InstanceType<typeof VeeForm> | null>(null)

watch(
  () => [props.defaultProjectId, props.projectOptions.length] as const,
  ([defaultProjectId]) => {
    if (!defaultProjectId || !hasProjectOptions.value) {
      return
    }

    taskForm.value?.setFieldValue('projectId', defaultProjectId)
  },
  { immediate: true },
)

async function handleValidSubmit(
  values: Record<string, unknown>,
  { resetForm }: SubmissionContext<Record<string, unknown>>,
) {
  if (!hasProjectOptions.value) {
    return
  }

  const formValues = toCreateTaskFormValues(values)
  const payload: SubmitTaskPayload = {
    projectId: formValues.projectId,
    title: formValues.title.trim(),
    description: formValues.description?.trim() || null,
    dueAt: formValues.dueAt ? new Date(formValues.dueAt).toISOString() : null,
    durationMinutes: nullableNumberValue(formValues.durationMinutes),
    priority: Number(formValues.priority),
    status: formValues.status,
  }

  await props.onSubmitTask(payload)
  resetForm({
    values: {
      ...DEFAULT_CREATE_TASK_FORM,
      projectId: props.defaultProjectId || DEFAULT_CREATE_TASK_FORM.projectId,
    },
  } satisfies Partial<FormState<Record<string, unknown>>>)
}

function toCreateTaskFormValues(values: Record<string, unknown>): CreateTaskFormValues {
  return {
    projectId: stringValue(values.projectId),
    title: stringValue(values.title),
    description: stringValue(values.description),
    priority: numberValue(values.priority, DEFAULT_CREATE_TASK_FORM.priority),
    durationMinutes: nullableNumberValue(values.durationMinutes),
    dueAt: stringValue(values.dueAt),
    status: isTaskStatus(values.status) ? values.status : DEFAULT_CREATE_TASK_FORM.status,
  }
}

function stringValue(value: unknown): string {
  return typeof value === 'string' ? value : ''
}

function numberValue(value: unknown, fallback: number): number {
  return typeof value === 'number' ? value : Number(value ?? fallback)
}

function nullableNumberValue(value: unknown): number | null {
  if (value === null || value === undefined || value === '') {
    return null
  }

  return typeof value === 'number' ? value : Number(value)
}

function isTaskStatus(value: unknown): value is CreateTaskFormValues['status'] {
  return typeof value === 'string' && ['TODO', 'IN_PROGRESS', 'DONE', 'ARCHIVED'].includes(value)
}
</script>

<template>
  <a-card title="Create task" class="surface-card" :bordered="false">
    <VeeForm
      ref="taskForm"
      :validation-schema="schema"
      :initial-values="initialValues"
      @submit="handleValidSubmit"
      v-slot="{ submitForm }"
    >
      <a-form layout="vertical" @submit.prevent="submitForm">
        <a-alert v-if="!hasProjectOptions" type="info" show-icon class="empty-projects-alert">
          <template #message>Create a project before adding tasks</template>
          <template #description>
            Tasks need a project so TaskMind can keep ownership, planning, and schedule context
            aligned.
            <RouterLink to="/projects">Create your first project</RouterLink>.
          </template>
        </a-alert>

        <a-row :gutter="12">
          <a-col :xs="24" :md="12">
            <a-form-item label="Project" required>
              <Field name="projectId" v-slot="{ value, handleChange }">
                <a-select
                  :value="value"
                  placeholder="Select a project"
                  :disabled="!hasProjectOptions"
                  @update:value="handleChange"
                >
                  <a-select-option
                    v-for="project in projectOptions"
                    :key="project.id"
                    :value="project.id"
                  >
                    {{ project.name }}
                  </a-select-option>
                </a-select>
              </Field>
              <ErrorMessage class="field-error" name="projectId" />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="12">
            <a-form-item label="Title" required>
              <Field name="title" v-slot="{ field }">
                <a-input v-bind="field" placeholder="Ship daily planner MVP" />
              </Field>
              <ErrorMessage class="field-error" name="title" />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="12">
            <a-form-item label="Status">
              <Field name="status" v-slot="{ value, handleChange }">
                <a-select :value="value" @update:value="handleChange">
                  <a-select-option value="TODO">TODO</a-select-option>
                  <a-select-option value="IN_PROGRESS">IN_PROGRESS</a-select-option>
                  <a-select-option value="DONE">DONE</a-select-option>
                  <a-select-option value="ARCHIVED">ARCHIVED</a-select-option>
                </a-select>
              </Field>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="Description">
          <Field name="description" v-slot="{ field }">
            <a-textarea v-bind="field" :rows="3" placeholder="Optional context" />
          </Field>
          <ErrorMessage class="field-error" name="description" />
        </a-form-item>

        <a-row :gutter="12">
          <a-col :xs="24" :md="8">
            <a-form-item label="Priority (1 highest)">
              <Field name="priority" v-slot="{ value, handleChange }">
                <a-input-number
                  :value="value"
                  :min="1"
                  :max="4"
                  :step="1"
                  style="width: 100%"
                  @update:value="handleChange"
                />
              </Field>
              <ErrorMessage class="field-error" name="priority" />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="8">
            <a-form-item label="Duration (minutes, optional)">
              <Field name="durationMinutes" v-slot="{ value, handleChange }">
                <a-input-number
                  :value="value"
                  :min="1"
                  :step="5"
                  style="width: 100%"
                  @update:value="handleChange"
                />
              </Field>
              <ErrorMessage class="field-error" name="durationMinutes" />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="8">
            <a-form-item label="Due at (optional)">
              <Field name="dueAt" v-slot="{ value, handleChange }">
                <a-date-picker
                  show-time
                  style="width: 100%"
                  format="YYYY-MM-DD HH:mm"
                  value-format="YYYY-MM-DDTHH:mm"
                  :value="value || undefined"
                  placeholder="Set due date"
                  @update:value="handleChange"
                />
              </Field>
              <ErrorMessage class="field-error" name="dueAt" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-button
          type="primary"
          html-type="submit"
          size="large"
          block
          :loading="saving"
          :disabled="!hasProjectOptions"
        >
          Create task
        </a-button>
      </a-form>
    </VeeForm>
  </a-card>
</template>

<style scoped>
.surface-card {
  border-radius: 16px;
  box-shadow: var(--tm-shadow-sm);
}

:deep(.ant-card-head-title) {
  font-size: 22px;
}

:deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: var(--tm-text);
}

.field-error {
  color: var(--tm-accent-orange);
  font-size: 12px;
}

.empty-projects-alert {
  margin-bottom: 16px;
}
</style>
