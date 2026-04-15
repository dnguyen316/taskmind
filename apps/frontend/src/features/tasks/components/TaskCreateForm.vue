<script setup>
import { useForm } from 'vee-validate'
import * as yup from 'yup'
import { DEFAULT_CREATE_TASK_FORM, TASK_STATUS_OPTIONS } from '../constants/taskConstants'

const props = defineProps({
  saving: {
    type: Boolean,
    default: false,
  },
  onSubmitTask: {
    type: Function,
    required: true,
  },
})

const schema = yup.object({
  title: yup.string().trim().required('Title is required').max(120, 'Max 120 characters'),
  description: yup.string().max(1000, 'Max 1000 characters').default(''),
  status: yup.string().oneOf(TASK_STATUS_OPTIONS).required(),
  priority: yup.number().required().min(1).max(5),
  durationMinutes: yup.number().required().min(5).max(480),
  dueAt: yup.string().nullable().default(''),
})

const { errors, defineField, handleSubmit, resetForm } = useForm({
  validationSchema: schema,
  initialValues: { ...DEFAULT_CREATE_TASK_FORM },
})

const [title] = defineField('title')
const [description] = defineField('description')
const [status] = defineField('status')
const [priority] = defineField('priority')
const [durationMinutes] = defineField('durationMinutes')
const [dueAt] = defineField('dueAt')

const onSubmit = handleSubmit(async (validatedValues) => {
  const result = await props.onSubmitTask(validatedValues)
  if (result?.ok) {
    resetForm({ values: { ...DEFAULT_CREATE_TASK_FORM } })
  }
})
</script>

<template>
  <a-card title="Create task">
    <a-form layout="vertical" @submit.prevent="onSubmit">
      <a-form-item label="Title" :validate-status="errors.title ? 'error' : ''" :help="errors.title">
        <a-input v-model:value="title" placeholder="Ship onboarding flow" />
      </a-form-item>

      <a-form-item label="Description" :validate-status="errors.description ? 'error' : ''" :help="errors.description">
        <a-textarea v-model:value="description" :rows="3" placeholder="Add details (optional)" />
      </a-form-item>

      <a-row :gutter="12">
        <a-col :xs="24" :md="12">
          <a-form-item label="Status" :validate-status="errors.status ? 'error' : ''" :help="errors.status">
            <a-select v-model:value="status" :options="TASK_STATUS_OPTIONS.map((value) => ({ value, label: value }))" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-form-item label="Priority (1-5)" :validate-status="errors.priority ? 'error' : ''" :help="errors.priority">
            <a-input-number v-model:value="priority" :min="1" :max="5" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="12">
        <a-col :xs="24" :md="12">
          <a-form-item
            label="Duration (minutes)"
            :validate-status="errors.durationMinutes ? 'error' : ''"
            :help="errors.durationMinutes"
          >
            <a-input-number v-model:value="durationMinutes" :min="5" :max="480" :step="5" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-form-item label="Due at" :validate-status="errors.dueAt ? 'error' : ''" :help="errors.dueAt">
            <a-input v-model:value="dueAt" type="datetime-local" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-button type="primary" html-type="submit" :loading="props.saving">Create task</a-button>
    </a-form>
  </a-card>
</template>
