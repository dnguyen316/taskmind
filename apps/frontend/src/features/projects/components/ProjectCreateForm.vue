<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Form as VeeForm, Field, ErrorMessage } from 'vee-validate'
import type { FormContext, GenericObject } from 'vee-validate'
import type { CreateProjectPayload } from '../types'
import * as yup from 'yup'

interface CreateProjectFormValues {
  name: string
  key: string
  ownerUserId: string
  description: string | null
}

const props = withDefaults(defineProps<{ saving?: boolean; successSignal?: number }>(), {
  saving: false,
  successSignal: 0,
})

const emit = defineEmits<{
  submit: [payload: CreateProjectPayload]
}>()

const schema = yup.object({
  name: yup
    .string()
    .trim()
    .required('Name is required')
    .max(200, 'Name must be at most 200 characters'),
  key: yup.string().trim().required('Key is required').max(50, 'Key must be at most 50 characters'),
  ownerUserId: yup
    .string()
    .trim()
    .required('Owner user id is required')
    .max(100, 'Owner user id must be at most 100 characters'),
  description: yup.string().max(2000, 'Description must be at most 2000 characters').nullable(),
})

const initialValues = computed<CreateProjectFormValues>(() => ({
  name: '',
  key: '',
  ownerUserId: '',
  description: null,
}))

const formRef = ref<FormContext | null>(null)

function handleValidSubmit(values: GenericObject) {
  emit('submit', {
    name: String(values.name ?? '').trim(),
    key: String(values.key ?? '').trim(),
    ownerUserId: String(values.ownerUserId ?? '').trim(),
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
  <a-card title="Create project" class="surface-card">
    <VeeForm
      ref="formRef"
      :validation-schema="schema"
      :initial-values="initialValues"
      @submit="handleValidSubmit"
      v-slot="{ submitForm }"
    >
      <a-form layout="vertical" @submit.prevent="submitForm">
        <a-row :gutter="12">
          <a-col :xs="24" :md="12">
            <a-form-item label="Name" required>
              <Field name="name" v-slot="{ field }">
                <a-input v-bind="field" placeholder="TaskMind Platform" />
              </Field>
              <ErrorMessage class="field-error" name="name" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="Key" required>
              <Field name="key" v-slot="{ field }">
                <a-input v-bind="field" placeholder="TASKMIND" />
              </Field>
              <ErrorMessage class="field-error" name="key" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="Owner user id" required>
          <Field name="ownerUserId" v-slot="{ field }">
            <a-input v-bind="field" placeholder="user_123" />
          </Field>
          <ErrorMessage class="field-error" name="ownerUserId" />
        </a-form-item>

        <a-form-item label="Description">
          <Field name="description" v-slot="{ field }">
            <a-textarea v-bind="field" :rows="3" placeholder="Optional description" />
          </Field>
          <ErrorMessage class="field-error" name="description" />
        </a-form-item>

        <a-button type="primary" html-type="submit" :loading="props.saving"
          >Create project</a-button
        >
      </a-form>
    </VeeForm>
  </a-card>
</template>

<style scoped>
.surface-card {
  border-radius: 18px;
}
.field-error {
  color: #d4380d;
  font-size: 12px;
}
</style>
