<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { SearchOutlined } from '@ant-design/icons-vue'
import { useActivitySearch } from '../composables/useActivitySearch'
import type { ActivitySearchDocument } from '../../tasks/types'

const route = useRoute()
const { query, size, loading, errorMessage, results, hasResults, runSearch, clearSearch } =
  useActivitySearch()

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

function submitSearch() {
  void runSearch()
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
          <a-input
            v-model:value="query"
            placeholder="Search activity, tasks, status changes..."
            allow-clear
          />
        </a-form-item>
        <a-form-item label="Size">
          <a-input-number v-model:value="size" :min="1" :max="100" :step="5" />
        </a-form-item>
        <a-button type="primary" html-type="submit" :loading="loading">
          <template #icon><SearchOutlined /></template>
          Search
        </a-button>
        <a-button :disabled="loading && !query" @click="clearSearch">Clear</a-button>
      </a-form>

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
