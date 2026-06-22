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
    showViewAll: true,
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
const STATUS_OPTION_PREFIX = '__taskmind_status__:'

type SuggestionOption = {
  value: string
  label: string
  disabled?: boolean
  recommendation?: ActivitySearchSuggestion
  kind: 'status' | 'recommendation' | 'viewAll'
}

const focused = ref(false)
const suggestions = ref<ActivitySearchSuggestion[]>([])
const suggestionsLoading = ref(false)
const suggestionsErrorMessage = ref('')
let suggestionTimer: ReturnType<typeof setTimeout> | undefined
let blurTimer: ReturnType<typeof setTimeout> | undefined
let suggestionRequestId = 0

const trimmedQuery = computed(() => props.value.trim())
const hasValidQuery = computed(() => trimmedQuery.value.length >= props.minLength)
const remainingCharacters = computed(() => Math.max(props.minLength - trimmedQuery.value.length, 0))
const showRecommendationDropdown = computed(() => focused.value)
const viewAllLabelText = computed(() => `View all results for “${trimmedQuery.value}”`)
const dropdownState = computed(() => {
  if (!hasValidQuery.value) {
    return 'too-short'
  }

  if (suggestionsLoading.value) {
    return 'loading'
  }

  if (suggestionsErrorMessage.value) {
    return 'error'
  }

  if (suggestions.value.length === 0) {
    return 'empty'
  }

  return 'matches'
})
const suggestionOptions = computed<SuggestionOption[]>(() => {
  if (!hasValidQuery.value) {
    const characterLabel = remainingCharacters.value === 1 ? 'character' : 'characters'
    return [
      {
        value: `${STATUS_OPTION_PREFIX}too-short`,
        label: `Type ${remainingCharacters.value} more ${characterLabel} to see recommendations.`,
        disabled: true,
        kind: 'status',
      },
    ]
  }

  const stateOptions: SuggestionOption[] = []

  if (dropdownState.value === 'loading') {
    stateOptions.push({
      value: `${STATUS_OPTION_PREFIX}loading`,
      label: 'Searching recommendations…',
      disabled: true,
      kind: 'status',
    })
  } else if (dropdownState.value === 'error') {
    stateOptions.push({
      value: `${STATUS_OPTION_PREFIX}error`,
      label: suggestionsErrorMessage.value,
      disabled: true,
      kind: 'status',
    })
  } else if (dropdownState.value === 'empty') {
    stateOptions.push({
      value: `${STATUS_OPTION_PREFIX}empty`,
      label: 'No recommendation matches. Run a full search instead.',
      disabled: true,
      kind: 'status',
    })
  } else {
    stateOptions.push(
      ...suggestions.value.map((suggestion) => ({
        value: suggestion.value,
        label: suggestion.label,
        recommendation: suggestion,
        kind: 'recommendation' as const,
      })),
    )
  }

  return [
    ...stateOptions,
    {
      value: `${VIEW_ALL_OPTION_PREFIX}${trimmedQuery.value}`,
      label: viewAllLabelText.value,
      kind: 'viewAll',
    },
  ]
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
  if (blurTimer) {
    clearTimeout(blurTimer)
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
  if (value.startsWith(STATUS_OPTION_PREFIX)) {
    return
  }

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

function focusRecommendationDropdown() {
  if (blurTimer) {
    clearTimeout(blurTimer)
  }
  focused.value = true
}

function closeRecommendationDropdown() {
  blurTimer = setTimeout(() => {
    focused.value = false
  }, 150)
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    focused.value = false
    return
  }

  if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
    focused.value = true
    return
  }
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
      @focus="focusRecommendationDropdown"
      @blur="closeRecommendationDropdown"
      @select="selectSuggestion"
    >
      <a-input
        :size="inputSize"
        :placeholder="placeholder"
        @keydown="handleKeydown"
        @press-enter="submitSearch()"
      >
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
        <div v-else-if="option.kind === 'viewAll'" class="recommendation-view-all">
          {{ option.label }}
        </div>
        <div v-else class="recommendation-status" :data-state="dropdownState">
          <a-spin v-if="dropdownState === 'loading'" size="small" />
          <span>{{ option.label }}</span>
        </div>
      </template>
      <template #notFoundContent>
        <div class="recommendation-empty">Type to search recommendations.</div>
      </template>
    </a-auto-complete>
  </div>
</template>

<style scoped>
.activity-search-autocomplete-shell,
.activity-search-autocomplete {
  width: 100%;
}

.recommendation-empty,
.recommendation-status,
.recommendation-view-all {
  padding: 8px 12px;
}

.recommendation-empty,
.recommendation-status {
  display: flex;
  gap: 8px;
  align-items: center;
  color: var(--tm-text-muted);
}

.recommendation-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.recommendation-label,
.recommendation-view-all {
  color: var(--tm-text);
  font-weight: 600;
}

.recommendation-view-all {
  border-top: 1px solid var(--tm-border-subtle);
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
