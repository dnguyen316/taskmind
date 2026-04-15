<script setup>
import { computed } from 'vue'
import { Form as VeeForm, Field, ErrorMessage } from 'vee-validate'
import * as yup from 'yup'
import { DEFAULT_CREATE_TASK_FORM } from '../constants/taskConstants'

const props = defineProps({
  saving: { type: Boolean, default: false },
  onSubmitTask: {
    type: Function,
    required: true,
  },
})

const schema = yup.object({
  title: yup.string().trim().required('Title is required').max(200, 'Title must be at most 200 characters'),
  description: yup.string().max(2000, 'Description must be at most 2000 characters').nullable(),
  priority: yup.number().integer().min(1).max(4).required(),
  durationMinutes: yup.number().integer().min(1).max(1440).required('Duration is required'),
  dueAt: yup.string().nullable(),
  status: yup.string().oneOf(['TODO', 'IN_PROGRESS', 'DONE', 'ARCHIVED']).required(),
})

const initialValues = computed(() => ({ ...DEFAULT_CREATE_TASK_FORM }))

async function handleValidSubmit(values, { resetForm }) {
  const payload = {
    ...values,
    title: values.title.trim(),
    description: values.description?.trim() || null,
    dueAt: values.dueAt ? new Date(values.dueAt).toISOString() : null,
    durationMinutes: Number(values.durationMinutes),
    priority: Number(values.priority),
  }

  await props.onSubmitTask(payload)
  resetForm({ values: { ...DEFAULT_CREATE_TASK_FORM } })
}
</script>

<template>
  <a-card title="Create Task">
    <VeeForm :validation-schema="schema" :initial-values="initialValues" @submit="handleValidSubmit" v-slot="{ submitForm }">
      <a-form layout="vertical" @submit.prevent="submitForm">
        <a-row :gutter="12">
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
                <a-input type="datetime-local" :value="value" @input="(event) => handleChange(event.target.value)" />
              </Field>
              <ErrorMessage class="field-error" name="dueAt" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-button type="primary" html-type="submit" :loading="saving">
          Create task
        </a-button>
      </a-form>
    </VeeForm>
  </a-card>
</template>

<style scoped>
.field-error {
  color: #d4380d;
  font-size: 12px;
}
</style>
