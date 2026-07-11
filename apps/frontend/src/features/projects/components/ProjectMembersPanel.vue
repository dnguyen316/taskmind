<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ErrorMessage, Field, Form as VeeForm } from 'vee-validate'
import type { GenericObject, SubmissionContext } from 'vee-validate'
import * as yup from 'yup'
import { getTeamDirectory } from '../../team/api/teamApi'
import type { TeamMember } from '../../team/types'
import type { AddProjectMemberPayload, ProjectMembership } from '../types'

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

const teamMembers = ref<TeamMember[]>([])
const loadingTeamMembers = ref(false)
const teamSelectorError = ref('')
const teamPermissionDenied = ref(false)

const schema = yup.object({
  userId: yup
    .string()
    .trim()
    .required('Team member is required')
    .max(100, 'User id must be at most 100 characters'),
  role: yup
    .string()
    .trim()
    .oneOf(['OWNER', 'ADMIN', 'MEMBER', 'VIEWER'])
    .required('Role is required'),
})

onMounted(() => {
  void fetchTeamMembers()
})

async function fetchTeamMembers() {
  loadingTeamMembers.value = true
  teamSelectorError.value = ''
  teamPermissionDenied.value = false

  try {
    teamMembers.value = (await getTeamDirectory()).members
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : 'Failed to load team members.'
    teamPermissionDenied.value = /403|forbidden|permission|not authorized/i.test(message)
    teamSelectorError.value = teamPermissionDenied.value
      ? 'You do not have permission to browse team members.'
      : message
  } finally {
    loadingTeamMembers.value = false
  }
}

function onAdd(values: GenericObject, { resetForm }: SubmissionContext) {
  emit('addMember', {
    userId: String(values.userId ?? '').trim(),
    role: String(values.role ?? '').trim() as AddProjectMemberPayload['role'],
  })
  resetForm({ values: { userId: '', role: 'MEMBER' } })
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
    <a-alert
      v-if="teamSelectorError"
      :type="teamPermissionDenied ? 'warning' : 'error'"
      show-icon
      :message="teamSelectorError"
      class="space-bottom"
    />

    <VeeForm
      v-slot="{ submitForm, setFieldValue, values }"
      :validation-schema="schema"
      :initial-values="{ userId: '', role: 'MEMBER' }"
      @submit="onAdd"
    >
      <a-form layout="inline" class="member-form" @submit.prevent="submitForm">
        <a-form-item label="Team member" required>
          <Field name="userId" type="hidden" />
          <a-select
            :value="values.userId"
            show-search
            allow-clear
            option-filter-prop="label"
            placeholder="Search team members"
            style="min-width: 260px"
            :loading="loadingTeamMembers"
            :disabled="loadingTeamMembers || teamPermissionDenied"
            :options="
              teamMembers.map((member) => ({
                value: member.userId,
                label: `${member.displayName} · ${member.email}`,
              }))
            "
            @change="(value: string | undefined) => setFieldValue('userId', value ?? '')"
          >
            <template #notFoundContent>
              <a-spin v-if="loadingTeamMembers" size="small" />
              <span v-else-if="teamPermissionDenied">Permission denied.</span>
              <span v-else>No team members available.</span>
            </template>
          </a-select>
          <ErrorMessage class="field-error" name="userId" />
        </a-form-item>

        <a-form-item label="Role" required>
          <Field name="role" v-slot="{ field }">
            <a-select v-bind="field" style="width: 140px" placeholder="Role">
              <a-select-option value="OWNER">Owner</a-select-option>
              <a-select-option value="ADMIN">Admin</a-select-option>
              <a-select-option value="MEMBER">Member</a-select-option>
              <a-select-option value="VIEWER">Viewer</a-select-option>
            </a-select>
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
        <template #renderItem="{ item }: { item: ProjectMembership }">
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
