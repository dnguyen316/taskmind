<script setup lang="ts">
import { Form as VeeForm, Field, ErrorMessage } from 'vee-validate'
import type { GenericObject, SubmissionContext } from 'vee-validate'
import type { AddProjectMemberPayload, ProjectMembership } from '../types'
import * as yup from 'yup'

const props = withDefaults(
  defineProps<{
    members: ProjectMembership[]
    loading?: boolean
    saving?: boolean
    errorMessage?: string | null
  }>(),
  {
    loading: false,
    saving: false,
    errorMessage: null,
  },
)

const emit = defineEmits<{
  addMember: [payload: AddProjectMemberPayload]
  removeMember: [userId: string]
}>()

const schema = yup.object({
  userId: yup
    .string()
    .trim()
    .required('User id is required')
    .max(100, 'User id must be at most 100 characters'),
  role: yup
    .string()
    .trim()
    .oneOf(['OWNER', 'ADMIN', 'MEMBER', 'VIEWER'])
    .required('Role is required'),
})

function onAdd(values: GenericObject, { resetForm }: SubmissionContext) {
  emit('addMember', {
    userId: String(values.userId ?? '').trim(),
    role: String(values.role ?? '').trim() as AddProjectMemberPayload['role'],
  })
  resetForm({ values: { userId: '', role: '' } })
}
</script>

<template>
  <a-card title="Project members" class="surface-card">
    <a-alert
      v-if="props.errorMessage"
      type="error"
      show-icon
      :message="props.errorMessage"
      class="space-bottom"
    />

    <VeeForm
      :validation-schema="schema"
      :initial-values="{ userId: '', role: '' }"
      @submit="onAdd"
      v-slot="{ submitForm }"
    >
      <a-form layout="inline" @submit.prevent="submitForm" class="member-form">
        <a-form-item label="User id" required>
          <Field name="userId" v-slot="{ field }">
            <a-input v-bind="field" placeholder="user_123" />
          </Field>
          <ErrorMessage class="field-error" name="userId" />
        </a-form-item>

        <a-form-item label="Role" required>
          <Field name="role" v-slot="{ field }">
            <a-input v-bind="field" placeholder="MAINTAINER" />
          </Field>
          <ErrorMessage class="field-error" name="role" />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" :loading="props.saving">Add member</a-button>
        </a-form-item>
      </a-form>
    </VeeForm>

    <a-spin :spinning="props.loading" class="members-spin">
      <a-empty v-if="!props.loading && props.members.length === 0" description="No members yet." />
      <a-list v-else :data-source="props.members" size="small">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-space>
              <span
                ><strong>{{ item.userId }}</strong></span
              >
              <a-tag>{{ item.role }}</a-tag>
            </a-space>
            <template #actions>
              <a-button danger type="link" @click="emit('removeMember', item.userId)"
                >Remove</a-button
              >
            </template>
          </a-list-item>
        </template>
      </a-list>
    </a-spin>
  </a-card>
</template>

<style scoped>
.surface-card {
  border-radius: 18px;
}
.space-bottom {
  margin-bottom: 12px;
}
.member-form {
  margin-bottom: 12px;
}
.members-spin {
  width: 100%;
}
.field-error {
  display: block;
  color: #d4380d;
  font-size: 12px;
}
</style>
