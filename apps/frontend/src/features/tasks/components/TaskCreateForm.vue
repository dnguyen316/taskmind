<script setup lang="ts">
import { computed } from 'vue'
import { Form as VeeForm, Field, ErrorMessage } from 'vee-validate'
import type { FormState, SubmissionContext } from 'vee-validate'
import * as yup from 'yup'
import { DEFAULT_CREATE_TASK_FORM } from '../constants/taskConstants'
import type { CreateTaskFormValues, CreateTaskPayload, Project } from '../types'

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
  title: yup.string().trim().required('Title is required').max(200, 'Title must be at most 200 characters'),
  description: yup.string().max(2000, 'Description must be at most 2000 characters').nullable(),
  priority: yup.number().integer().min(1).max(4).required(),
  durationMinutes: yup.number().integer().min(1).max(1440).required('Duration is required'),
  dueAt: yup.string().nullable(),
  status: yup.string().oneOf(['TODO', 'IN_PROGRESS', 'DONE', 'ARCHIVED']).required(),
})

const initialValues = computed<CreateTaskFormValues>(() => ({
  ...DEFAULT_CREATE_TASK_FORM,
  projectId: props.defaultProjectId || DEFAULT_CREATE_TASK_FORM.projectId,
}))

async function handleValidSubmit(values: Record<string, unknown>, { resetForm }: SubmissionContext<Record<string, unknown>>) {
  const formValues = toCreateTaskFormValues(values)
  const payload: SubmitTaskPayload = {
    projectId: formValues.projectId,
    title: formValues.title.trim(),
    description: formValues.description?.trim() || null,
    dueAt: formValues.dueAt ? new Date(formValues.dueAt).toISOString() : null,
    durationMinutes: Number(formValues.durationMinutes),
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
    durationMinutes: numberValue(values.durationMinutes, DEFAULT_CREATE_TASK_FORM.durationMinutes),
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

function isTaskStatus(value: unknown): value is CreateTaskFormValues['status'] {
  return typeof value === 'string' && ['TODO', 'IN_PROGRESS', 'DONE', 'ARCHIVED'].includes(value)
}
</script>


<template>
  <a-card title="Create task" class="surface-card" :bordered="false">
    <VeeForm :validation-schema="schema" :initial-values="initialValues" @submit="handleValidSubmit" v-slot="{ submitForm }">
      <a-form layout="vertical" @submit.prevent="submitForm">
        <a-row :gutter="12">
          <a-col :xs="24" :md="12">
            <a-form-item label="Project" required>
              <Field name="projectId" v-slot="{ value, handleChange }">
                <a-select :value="value" placeholder="Select a project" @update:value="handleChange">
                  <a-select-option v-for="project in projectOptions" :key="project.id" :value="project.id">
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
                <a-input-number :value="value" :min="1" :max="4" :step="1" style="width: 100%" @update:value="handleChange" />
              </Field>
              <ErrorMessage class="field-error" name="priority" />
            </a-form-item>
          </a-col>

          <a-col :xs="24" :md="8">
            <a-form-item label="Duration (minutes)">
              <Field name="durationMinutes" v-slot="{ value, handleChange }">
                <a-input-number :value="value" :min="1" :step="5" style="width: 100%" @update:value="handleChange" />
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

        <a-button type="primary" html-type="submit" size="large" block :loading="saving">
          Create task
        </a-button>
      </a-form>
    </VeeForm>
  </a-card>
</template>

<style scoped>
.surface-card {
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

:deep(.ant-card-head-title) {
  font-size: 22px;
}

:deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: #0f172a;
}

.field-error {
  color: #d4380d;
  font-size: 12px;
}
</style>
