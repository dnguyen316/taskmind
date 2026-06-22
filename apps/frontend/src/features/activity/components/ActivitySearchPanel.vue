<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { RobotOutlined, SearchOutlined } from '@ant-design/icons-vue'
import ActivitySearchAutocomplete from './ActivitySearchAutocomplete.vue'
import { useActivitySearch } from '../composables/useActivitySearch'
import type { ActivitySearchDocument } from '../../tasks/types'

const route = useRoute()
const {
  query,
  size,
  entityType,
  status,
  projectId,
  from,
  to,
  eventType,
  loading,
  errorMessage,
  results,
  aiLoading,
  aiErrorMessage,
  aiProposal,
  hasResults,
  askNova,
  applyAiProposal,
  dismissAiProposal,
  runSearch,
  clearSearch,
} = useActivitySearch()

function applyRouteQuery() {
  const routeQuery = route.query.q
  query.value = Array.isArray(routeQuery) ? (routeQuery[0] ?? '') : (routeQuery ?? '')
}

onMounted(() => {
  applyRouteQuery()
  void runSearch()
})

watch(
  () => route.query.q,
  () => {
    applyRouteQuery()
    void runSearch()
  },
)

const searchFilters = computed(() => ({
  entityType: entityType.value,
  status: status.value,
  projectId: projectId.value,
  from: from.value ? new Date(from.value).toISOString() : undefined,
  to: to.value ? new Date(to.value).toISOString() : undefined,
  eventType: eventType.value,
}))

function submitSearch() {
  void runSearch()
}

function selectSuggestion(value: string) {
  query.value = value
}

function formatEventType(value: string) {
  return value.split('.').join(' · ')
}

function formatDate(value: string) {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return value
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

function documentTitle(document: ActivitySearchDocument) {
  return document.title || `${document.entityType} ${document.entityId}`
}

function payloadPreview(document: ActivitySearchDocument) {
  if (!document.payload || Object.keys(document.payload).length === 0) {
    return null
  }

  return JSON.stringify(document.payload)
}
</script>

<template>
  <a-card class="activity-search-panel" title="Activity search">
    <a-space direction="vertical" size="middle" style="width: 100%">
      <a-form layout="inline" class="activity-search-form" @submit.prevent="submitSearch">
        <a-form-item label="Query">
          <ActivitySearchAutocomplete
            v-model:value="query"
            :filters="searchFilters"
            placeholder="Search activity, tasks, status changes..."
            @select-suggestion="selectSuggestion"
            @submit-search="submitSearch"
            @view-all="submitSearch"
          />
        </a-form-item>

        <a-form-item label="Entity">
          <a-select
            v-model:value="entityType"
            allow-clear
            placeholder="Any"
            style="width: 120px"
            :options="[
              { value: 'task', label: 'Task' },
              { value: 'project', label: 'Project' },
              { value: 'attachment', label: 'Attachment' },
              { value: 'document', label: 'Document' },
              { value: 'spec', label: 'Spec' },
              { value: 'spec-document', label: 'Spec document' },
            ]"
          />
        </a-form-item>
        <a-form-item label="Status">
          <a-input
            v-model:value="status"
            allow-clear
            placeholder="Any status"
            style="width: 130px"
          />
        </a-form-item>
        <a-form-item label="Event">
          <a-input
            v-model:value="eventType"
            allow-clear
            placeholder="task.updated"
            style="width: 150px"
          />
        </a-form-item>
        <a-form-item label="Project">
          <a-input
            v-model:value="projectId"
            allow-clear
            placeholder="Project UUID"
            style="width: 220px"
          />
        </a-form-item>
        <a-form-item label="From">
          <a-input v-model:value="from" type="datetime-local" style="width: 190px" />
        </a-form-item>
        <a-form-item label="To">
          <a-input v-model:value="to" type="datetime-local" style="width: 190px" />
        </a-form-item>
        <a-form-item label="Size">
          <a-input-number v-model:value="size" :min="1" :max="100" :step="5" />
        </a-form-item>
        <a-button type="primary" html-type="submit" :loading="loading">
          <template #icon><SearchOutlined /></template>
          Search
        </a-button>
        <a-button :loading="aiLoading" :disabled="loading || !query.trim()" @click="askNova">
          <template #icon><RobotOutlined /></template>
          Ask Nova
        </a-button>
        <a-button :disabled="loading && !query" @click="clearSearch">Clear</a-button>
      </a-form>

      <a-alert v-if="aiErrorMessage" type="error" show-icon :message="aiErrorMessage" />

      <a-card v-if="aiProposal" size="small" class="ai-proposal-card" title="Nova refined query">
        <a-space direction="vertical" size="small" style="width: 100%">
          <a-typography-text code>{{ aiProposal.query }}</a-typography-text>
          <a-typography-text v-if="aiProposal.explanation" type="secondary">
            {{ aiProposal.explanation }}
          </a-typography-text>
          <a-space v-if="aiProposal.suggestedFilters.length" wrap>
            <a-tag v-for="filter in aiProposal.suggestedFilters" :key="filter">{{ filter }}</a-tag>
          </a-space>
          <a-space>
            <a-button type="primary" @click="applyAiProposal">Apply and search</a-button>
            <a-button @click="dismissAiProposal">Dismiss</a-button>
          </a-space>
        </a-space>
      </a-card>

      <a-alert v-if="errorMessage" type="error" show-icon :message="errorMessage" />

      <a-spin v-if="loading" tip="Searching activity..." />

      <a-empty v-else-if="!hasResults" description="No matching activity events." />

      <a-list v-else item-layout="vertical" :data-source="results">
        <template #renderItem="{ item }">
          <a-list-item :key="item.eventId" class="activity-search-item">
            <template #extra>
              <a-tag>{{ item.entityType }}</a-tag>
            </template>
            <a-list-item-meta>
              <template #title>
                <span>{{ documentTitle(item) }}</span>
              </template>
              <template #description>
                <span>{{ formatEventType(item.eventType) }}</span>
                <span v-if="item.status"> · {{ item.status }}</span>
                <span> · {{ formatDate(item.occurredAt) }}</span>
              </template>
            </a-list-item-meta>
            <p v-if="payloadPreview(item)" class="payload-preview">{{ payloadPreview(item) }}</p>
          </a-list-item>
        </template>
      </a-list>
    </a-space>
  </a-card>
</template>

<style scoped>
.activity-search-panel {
  border-radius: 18px;
}

.activity-search-form {
  row-gap: 12px;
}

.activity-search-form :deep(.ant-form-item:first-child) {
  flex: 1 1 320px;
}

.activity-search-form :deep(.ant-form-item:first-child .ant-form-item-control) {
  min-width: 280px;
}

.ai-proposal-card {
  background: var(--tm-surface-muted);
  border-radius: 14px;
}

.activity-search-item {
  border-radius: 12px;
}

.payload-preview {
  padding: 10px 12px;
  overflow: hidden;
  color: var(--tm-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
  background: var(--tm-surface-muted);
  border-radius: 10px;
}
</style>
