<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Form as VeeForm, Field, ErrorMessage } from 'vee-validate'
import type { FormContext, GenericObject } from 'vee-validate'
import type { CreateProjectPayload } from '../types'
import * as yup from 'yup'

interface CreateProjectFormValues {
  name: string
  key: string
  description: string | null
}

const props = withDefaults(
  defineProps<{ saving?: boolean; successSignal?: number; currentUserLabel?: string }>(),
  {
    saving: false,
    successSignal: 0,
    currentUserLabel: 'your account',
  },
)

const emit = defineEmits<{
  submit: [payload: CreateProjectPayload]
}>()

const schema = yup.object({
  name: yup
    .string()
    .trim()
    .required('Name is required')
    .max(200, 'Name must be at most 200 characters'),
  key: yup
    .string()
    .trim()
    .required('Key is required')
    .matches(
      /^[A-Z][A-Z0-9]{1,19}$/,
      'Use 2-20 uppercase letters or numbers, starting with a letter',
    ),
  description: yup.string().max(2000, 'Description must be at most 2000 characters').nullable(),
})

const initialValues = computed<CreateProjectFormValues>(() => ({
  name: '',
  key: '',
  description: null,
}))

const formRef = ref<FormContext | null>(null)

function handleValidSubmit(values: GenericObject) {
  emit('submit', {
    name: String(values.name ?? '').trim(),
    key: String(values.key ?? '')
      .trim()
      .toUpperCase(),
    description: String(values.description ?? '').trim() || null,
  })
}

watch(
  () => props.successSignal,
  () => {
    formRef.value?.resetForm({ values: initialValues.value })
  },
)
</script>

<template>
  <a-card class="project-create-card tm-card-surface" :bordered="false">
    <div class="form-heading">
      <div>
        <p class="eyebrow">New workspace</p>
        <h2>Create project</h2>
        <p>
          Projects are created under {{ props.currentUserLabel }}. Add a short key so tasks can be
          referenced consistently across TaskMind.
        </p>
      </div>
    </div>

    <VeeForm
      ref="formRef"
      :validation-schema="schema"
      :initial-values="initialValues"
      @submit="handleValidSubmit"
      v-slot="{ submitForm }"
    >
      <a-form layout="vertical" class="project-form" @submit.prevent="submitForm">
        <a-row :gutter="16">
          <a-col :xs="24" :lg="14">
            <a-form-item label="Project name" required>
              <Field name="name" v-slot="{ field }">
                <a-input v-bind="field" size="large" placeholder="TaskMind Platform" />
              </Field>
              <ErrorMessage class="field-error" name="name" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :lg="10">
            <a-form-item label="Project key" required>
              <Field name="key" v-slot="{ field, handleChange }">
                <a-input
                  v-bind="field"
                  size="large"
                  placeholder="TASKMIND"
                  @change="handleChange(String($event.target.value || '').toUpperCase())"
                />
              </Field>
              <ErrorMessage class="field-error" name="key" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="Description">
          <Field name="description" v-slot="{ field }">
            <a-textarea
              v-bind="field"
              :rows="4"
              placeholder="What outcome will this project drive?"
            />
          </Field>
          <ErrorMessage class="field-error" name="description" />
        </a-form-item>

        <div class="form-actions">
          <a-button type="primary" size="large" html-type="submit" :loading="props.saving">
            Create project
          </a-button>
        </div>
      </a-form>
    </VeeForm>
  </a-card>
</template>

<style scoped>
.project-create-card {
  border-radius: 22px;
}

.form-heading {
  margin-bottom: 18px;
}

.form-heading h2 {
  margin: 4px 0 6px;
  color: var(--tm-text);
  font-size: 1.35rem;
}

.form-heading p:not(.eyebrow) {
  margin: 0;
  color: var(--tm-text-muted);
}

.eyebrow {
  margin: 0;
  color: var(--tm-accent-blue-strong);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.project-form {
  display: grid;
  gap: 2px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

.field-error {
  color: #d4380d;
  font-size: 12px;
}
</style>
