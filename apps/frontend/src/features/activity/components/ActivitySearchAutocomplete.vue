<script setup lang="ts">
import { computed, onScopeDispose, ref, watch } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import {
  recommendActivitySearch,
  type ActivitySearchFilters,
} from '../../tasks/api/activitySearchApi'
import type { ActivitySearchSuggestion } from '../../tasks/types'

const props = withDefaults(
  defineProps<{
    value: string
    filters?: ActivitySearchFilters
    placeholder?: string
    inputSize?: 'small' | 'middle' | 'large'
    suggestionLimit?: number
    minLength?: number
    debounceMs?: number
    allowClear?: boolean
    showViewAll?: boolean
    viewAllLabel?: string
  }>(),
  {
    filters: () => ({}),
    placeholder: 'Search activity, tasks, projects...',
    inputSize: 'middle',
    suggestionLimit: 8,
    minLength: 2,
    debounceMs: 250,
    allowClear: true,
    showViewAll: false,
    viewAllLabel: 'View all results',
  },
)

const emit = defineEmits<{
  'update:value': [value: string]
  selectSuggestion: [value: string, recommendation?: ActivitySearchSuggestion]
  submitSearch: [value: string]
  viewAll: [value: string]
}>()

const VIEW_ALL_OPTION_PREFIX = '__taskmind_view_all__:'

const focused = ref(false)
const suggestions = ref<ActivitySearchSuggestion[]>([])
const suggestionsLoading = ref(false)
const suggestionsErrorMessage = ref('')
let suggestionTimer: ReturnType<typeof setTimeout> | undefined
let suggestionRequestId = 0

const trimmedQuery = computed(() => props.value.trim())
const showRecommendationDropdown = computed(
  () => focused.value && trimmedQuery.value.length >= props.minLength,
)
const viewAllLabelText = computed(() => `${props.viewAllLabel} for “${trimmedQuery.value}”`)
const suggestionOptions = computed(() => {
  if (trimmedQuery.value.length < props.minLength) {
    return []
  }

  const viewAllOption = props.showViewAll
    ? [{ value: `${VIEW_ALL_OPTION_PREFIX}${trimmedQuery.value}`, label: viewAllLabelText.value }]
    : []

  if (suggestionsLoading.value) {
    return [
      { value: 'activity-search-loading', label: 'Searching…', disabled: true },
      ...viewAllOption,
    ]
  }

  if (suggestionsErrorMessage.value) {
    return [
      { value: 'activity-search-error', label: suggestionsErrorMessage.value, disabled: true },
      ...viewAllOption,
    ]
  }

  const options = suggestions.value.map((suggestion) => ({
    value: suggestion.value,
    label: suggestion.label,
    recommendation: suggestion,
  }))

  if (options.length === 0 && props.showViewAll) {
    return [
      { value: 'activity-search-empty', label: 'No matches found.', disabled: true },
      ...viewAllOption,
    ]
  }

  return [...options, ...viewAllOption]
})

async function loadSuggestions() {
  const suggestionQuery = trimmedQuery.value
  suggestionRequestId += 1
  const requestId = suggestionRequestId

  if (suggestionQuery.length < props.minLength) {
    suggestions.value = []
    suggestionsErrorMessage.value = ''
    suggestionsLoading.value = false
    return
  }

  suggestionsLoading.value = true
  suggestionsErrorMessage.value = ''

  try {
    const nextSuggestions = await recommendActivitySearch({
      query: suggestionQuery,
      size: props.suggestionLimit,
      ...props.filters,
    })
    if (requestId === suggestionRequestId) {
      suggestions.value = nextSuggestions
    }
  } catch (error: unknown) {
    if (requestId === suggestionRequestId) {
      suggestions.value = []
      suggestionsErrorMessage.value =
        error instanceof Error ? error.message : 'Failed to load activity suggestions.'
    }
  } finally {
    if (requestId === suggestionRequestId) {
      suggestionsLoading.value = false
    }
  }
}

watch(
  () => [props.value, props.filters] as const,
  () => {
    if (suggestionTimer) {
      clearTimeout(suggestionTimer)
    }
    suggestionTimer = setTimeout(() => {
      void loadSuggestions()
    }, props.debounceMs)
  },
  { deep: true },
)

onScopeDispose(() => {
  if (suggestionTimer) {
    clearTimeout(suggestionTimer)
  }
})

function updateValue(value: string) {
  emit('update:value', value)
}

function submitSearch(value = props.value) {
  const query = value.trim()

  if (!query) {
    return
  }

  focused.value = false
  emit('submitSearch', query)
}

function selectSuggestion(value: string) {
  if (value.startsWith(VIEW_ALL_OPTION_PREFIX)) {
    focused.value = false
    emit('viewAll', value.slice(VIEW_ALL_OPTION_PREFIX.length))
    return
  }

  const recommendation = suggestions.value.find((suggestion) => suggestion.value === value)
  updateValue(value)
  focused.value = false
  emit('selectSuggestion', value, recommendation)
}

function recommendationMeta(recommendation: ActivitySearchSuggestion) {
  return [recommendation.entityType, recommendation.status].filter(Boolean).join(' · ')
}

function recommendationTarget(recommendation: ActivitySearchSuggestion) {
  return recommendation.routeName ? 'Open item' : 'Use search term'
}

function closeRecommendationDropdown() {
  window.setTimeout(() => {
    focused.value = false
  }, 150)
}
</script>

<template>
  <div class="activity-search-autocomplete-shell">
    <a-auto-complete
      :value="value"
      class="activity-search-autocomplete"
      :options="suggestionOptions"
      :open="showRecommendationDropdown"
      :allow-clear="allowClear"
      @update:value="updateValue"
      @focus="focused = true"
      @blur="closeRecommendationDropdown"
      @select="selectSuggestion"
    >
      <a-input :size="inputSize" :placeholder="placeholder" @press-enter="submitSearch()">
        <template #prefix><SearchOutlined /></template>
      </a-input>
      <template #option="option">
        <div v-if="option.recommendation" class="recommendation-option">
          <div>
            <div class="recommendation-label">{{ option.recommendation.label }}</div>
            <div class="recommendation-meta">{{ recommendationMeta(option.recommendation) }}</div>
          </div>
          <span class="recommendation-target">{{
            recommendationTarget(option.recommendation)
          }}</span>
        </div>
        <span v-else>{{ option.label }}</span>
      </template>
      <template #notFoundContent>
        <div class="recommendation-empty">
          <a-spin v-if="suggestionsLoading" size="small" />
          <span v-else-if="suggestionsErrorMessage">{{ suggestionsErrorMessage }}</span>
          <span v-else>No recommendations yet.</span>
        </div>
      </template>
    </a-auto-complete>
  </div>
</template>

<style scoped>
.activity-search-autocomplete-shell,
.activity-search-autocomplete {
  width: 100%;
}

.recommendation-empty {
  padding: 8px 12px;
  color: var(--tm-text-muted);
}

.recommendation-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.recommendation-label {
  color: var(--tm-text);
  font-weight: 600;
}

.recommendation-meta,
.recommendation-target {
  color: var(--tm-text-muted);
  font-size: 12px;
}

.recommendation-target {
  white-space: nowrap;
}
</style>
