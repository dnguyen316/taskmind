<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import {
  discoverGitHubRepository,
  linkGitHubRepository,
  listConnections,
  listProjectLinks,
} from '../api/integrationsApi'
import type {
  GitHubAllowedOperation,
  GitHubRepository,
  IntegrationConnection,
  IntegrationProjectLink,
} from '../types'
const props = defineProps<{ projectId: string; canManage?: boolean }>()
const connections = ref<IntegrationConnection[]>([])
const links = ref<IntegrationProjectLink[]>([])
const repository = ref<GitHubRepository | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const notice = ref('')
const form = reactive({
  connectionId: '',
  owner: '',
  repo: '',
  allowedOperations: [
    'READ_ISSUES',
    'READ_CODE',
    'CREATE_BRANCH',
    'CREATE_PR',
    'COMMENT',
  ] as GitHubAllowedOperation[],
})
async function load() {
  if (!props.projectId) return
  loading.value = true
  error.value = ''
  try {
    const [connectionList, linkList] = await Promise.all([
      listConnections(),
      listProjectLinks(props.projectId),
    ])
    connections.value = connectionList.filter((connection) => connection.provider === 'GITHUB')
    links.value = linkList.filter((link) => link.provider === 'GITHUB')
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Unable to load GitHub integration status.'
  } finally {
    loading.value = false
  }
}
async function discover() {
  error.value = ''
  repository.value = null
  try {
    repository.value = await discoverGitHubRepository(
      form.connectionId,
      form.owner.trim(),
      form.repo.trim(),
    )
  } catch (e) {
    error.value =
      e instanceof Error ? e.message : 'Repository discovery failed. Check permissions and retry.'
  }
}
async function save() {
  saving.value = true
  error.value = ''
  try {
    const link = await linkGitHubRepository(props.projectId, {
      connectionId: form.connectionId,
      owner: form.owner.trim(),
      repo: form.repo.trim(),
      allowedOperations: form.allowedOperations,
    })
    links.value.unshift(link)
    notice.value = 'GitHub repository linked.'
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Repository link failed. Please retry.'
  } finally {
    saving.value = false
  }
}
function allowed(link: IntegrationProjectLink) {
  try {
    const parsed = JSON.parse(link.allowedOperationsJson || '[]')
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}
onMounted(load)
watch(() => props.projectId, load)
</script>
<template>
  <a-card title="GitHub repository links" class="github-card"
    ><a-space direction="vertical" style="width: 100%" size="middle"
      ><a-alert
        v-if="error"
        type="warning"
        show-icon
        message="Partial or recoverable error"
        :description="error"
      /><a-alert v-if="notice" type="success" show-icon :message="notice" /><a-spin
        v-if="loading"
        tip="Loading repository links..."
      /><template v-else
        ><a-empty
          v-if="!links.length"
          description="No GitHub repositories are linked to this project."
        /><a-list v-else bordered :data-source="links"
          ><template #renderItem="{ item }"
            ><a-list-item
              ><a-list-item-meta
                :title="`${item.repositoryOwner || 'unknown'}/${item.repositoryName || 'unknown'}`"
                :description="`Default branch: ${item.defaultBranch || 'unknown'}`"
              /><a-space wrap
                ><a-tag color="green">connected</a-tag
                ><a-tag v-for="operation in allowed(item)" :key="operation">{{
                  operation
                }}</a-tag></a-space
              ></a-list-item
            ></template
          ></a-list
        ><a-alert
          v-if="!canManage"
          type="info"
          show-icon
          message="Only project admins can link repositories."
        /><a-form v-else layout="vertical" @submit.prevent="save"
          ><a-row :gutter="12"
            ><a-col :xs="24" :md="8"
              ><a-form-item label="GitHub connection"
                ><a-select v-model:value="form.connectionId" placeholder="Connected GitHub account"
                  ><a-select-option
                    v-for="connection in connections"
                    :key="connection.id"
                    :value="connection.id"
                    >{{
                      connection.displayName ||
                      connection.accountName ||
                      connection.accountId ||
                      connection.id
                    }}</a-select-option
                  ></a-select
                ></a-form-item
              ></a-col
            ><a-col :xs="24" :md="8"
              ><a-form-item label="Owner"
                ><a-input v-model:value="form.owner" placeholder="taskmind" /></a-form-item></a-col
            ><a-col :xs="24" :md="8"
              ><a-form-item label="Repository"
                ><a-input
                  v-model:value="form.repo"
                  placeholder="web" /></a-form-item></a-col></a-row
          ><a-form-item label="Allowed operations"
            ><a-select v-model:value="form.allowedOperations" mode="multiple"
              ><a-select-option value="READ_ISSUES">Read issues</a-select-option
              ><a-select-option value="READ_CODE">Read code</a-select-option
              ><a-select-option value="CREATE_BRANCH">Create branch</a-select-option
              ><a-select-option value="CREATE_PR">Create PR</a-select-option
              ><a-select-option value="COMMENT">Comment</a-select-option></a-select
            ></a-form-item
          ><a-space
            ><a-button @click="discover" :disabled="!form.connectionId || !form.owner || !form.repo"
              >Check repository status</a-button
            ><a-button
              type="primary"
              html-type="submit"
              :loading="saving"
              :disabled="!form.connectionId || !form.owner || !form.repo"
              >Link repository</a-button
            ></a-space
          ><a-descriptions
            v-if="repository"
            bordered
            size="small"
            title="Connected repository status"
            ><a-descriptions-item label="Repository">{{ repository.fullName }}</a-descriptions-item
            ><a-descriptions-item label="Visibility">{{
              repository.isPrivate ? 'Private' : 'Public'
            }}</a-descriptions-item
            ><a-descriptions-item label="Default branch">{{
              repository.defaultBranch
            }}</a-descriptions-item
            ><a-descriptions-item label="URL"
              ><a :href="repository.htmlUrl" target="_blank" rel="noreferrer">{{
                repository.htmlUrl
              }}</a></a-descriptions-item
            ></a-descriptions
          ></a-form
        ></template
      ></a-space
    ></a-card
  >
</template>
<style scoped>
.github-card {
  border-radius: 18px;
}
</style>
