<script setup lang="ts">
import { computed } from 'vue'
import { Form as VeeForm, Field, ErrorMessage } from 'vee-validate'
import * as yup from 'yup'

interface ProjectEditableFields {
  name: string
  key: string
  description: string | null
}

const props = withDefaults(defineProps<{
  value: ProjectEditableFields
  saving?: boolean
}>(), {
  saving: false,
})

const emit = defineEmits<{
  submit: [payload: ProjectEditableFields]
}>()

const schema = yup.object({
  name: yup.string().trim().required('Name is required').max(200, 'Name must be at most 200 characters'),
  key: yup.string().trim().required('Key is required').max(50, 'Key must be at most 50 characters'),
  description: yup.string().max(2000, 'Description must be at most 2000 characters').nullable(),
})

const initialValues = computed<ProjectEditableFields>(() => ({
  name: props.value?.name ?? '',
  key: props.value?.key ?? '',
  description: props.value?.description ?? null,
}))

function handleValidSubmit(values: Record<string, unknown>) {
  emit('submit', {
    name: String(values.name ?? "").trim(),
    key: String(values.key ?? "").trim(),
    description: String(values.description ?? "").trim() || null,
  })
}
</script>

<template>
  <a-card title="Edit project" class="surface-card">
    <VeeForm :validation-schema="schema" :initial-values="initialValues" @submit="handleValidSubmit" v-slot="{ submitForm }">
      <a-form layout="vertical" @submit.prevent="submitForm">
        <a-row :gutter="12">
          <a-col :xs="24" :md="12">
            <a-form-item label="Name" required>
              <Field name="name" v-slot="{ field }">
                <a-input v-bind="field" />
              </Field>
              <ErrorMessage class="field-error" name="name" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="Key" required>
              <Field name="key" v-slot="{ field }">
                <a-input v-bind="field" />
              </Field>
              <ErrorMessage class="field-error" name="key" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="Description">
          <Field name="description" v-slot="{ field }">
            <a-textarea v-bind="field" :rows="3" />
          </Field>
          <ErrorMessage class="field-error" name="description" />
        </a-form-item>

        <a-button type="primary" html-type="submit" :loading="props.saving">Save changes</a-button>
      </a-form>
    </VeeForm>
  </a-card>
</template>

<style scoped>
.surface-card { border-radius: 18px; }
.field-error { color: #d4380d; font-size: 12px; }
</style>
