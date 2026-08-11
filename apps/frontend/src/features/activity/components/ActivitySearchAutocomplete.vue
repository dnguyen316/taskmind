<script setup lang="ts">
import { computed, onScopeDispose, ref, watch } from 'vue'
import {
  ArrowRightOutlined,
  CheckSquareOutlined,
  FolderOpenOutlined,
  HistoryOutlined,
  RobotOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'
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
    appearance?: 'standard' | 'ai'
    presentation?: 'standard' | 'compact'
    showShortcutHint?: boolean
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
    appearance: 'standard',
    presentation: 'standard',
    showShortcutHint: false,
  },
)

const emit = defineEmits<{
  'update:value': [value: string]
  selectSuggestion: [value: string, recommendation?: ActivitySearchSuggestion]
  submitSearch: [value: string]
  viewAll: [value: string]
  recommendationError: [message: string]
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
const shell = ref<HTMLElement | null>(null)
const suggestions = ref<ActivitySearchSuggestion[]>([])
const suggestionsLoading = ref(false)
let suggestionTimer: ReturnType<typeof setTimeout> | undefined
let blurTimer: ReturnType<typeof setTimeout> | undefined
let suggestionRequestId = 0

const trimmedQuery = computed(() => props.value.trim())
const hasValidQuery = computed(() => trimmedQuery.value.length >= props.minLength)
const remainingCharacters = computed(() => Math.max(props.minLength - trimmedQuery.value.length, 0))
const showRecommendationDropdown = computed(() => focused.value)
const viewAllLabelText = computed(() => `${props.viewAllLabel} for “${trimmedQuery.value}”`)
const dropdownState = computed(() => {
  if (!hasValidQuery.value) {
    return 'too-short'
  }

  if (suggestionsLoading.value) {
    return 'loading'
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

  if (!props.showViewAll) return stateOptions

  return [
    ...stateOptions,
    {
      value: `${VIEW_ALL_OPTION_PREFIX}${trimmedQuery.value}`,
      label: viewAllLabelText.value,
      kind: 'viewAll',
    },
  ]
})

async function loadSuggestions(requestId = ++suggestionRequestId) {
  const suggestionQuery = trimmedQuery.value

  if (suggestionQuery.length < props.minLength) {
    suggestions.value = []
    suggestionsLoading.value = false
    return
  }

  suggestionsLoading.value = true

  try {
    const nextSuggestions = await recommendActivitySearch({
      query: suggestionQuery,
      size: props.suggestionLimit,
      ...props.filters,
    })
    if (requestId === suggestionRequestId) {
      suggestions.value = nextSuggestions
    }
  } catch {
    if (requestId === suggestionRequestId) {
      suggestions.value = []
      emit('recommendationError', 'Recommendations couldn’t be loaded. You can still search.')
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
    const requestId = ++suggestionRequestId
    if (suggestionTimer) {
      clearTimeout(suggestionTimer)
    }
    suggestionTimer = setTimeout(() => {
      void loadSuggestions(requestId)
    }, props.debounceMs)
  },
  { deep: true },
)

onScopeDispose(() => {
  suggestionRequestId += 1
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

function recommendationIcon(entityType: string) {
  if (entityType === 'task') return CheckSquareOutlined
  if (entityType === 'project') return FolderOpenOutlined
  return HistoryOutlined
}

function focusInput() {
  shell.value?.querySelector<HTMLInputElement>('input')?.focus()
}

defineExpose({ focusInput })

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
  <div
    ref="shell"
    class="activity-search-autocomplete-shell"
    :class="{
      'is-ai': appearance === 'ai',
      'is-compact': presentation === 'compact',
    }"
  >
    <a-auto-complete
      :value="value"
      class="activity-search-autocomplete"
      :options="suggestionOptions"
      :open="showRecommendationDropdown"
      :allow-clear="allowClear"
      :popup-class-name="`activity-search-popup activity-search-popup--${appearance}`"
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
        <template #prefix>
          <span v-if="appearance === 'ai'" class="search-ai-mark" aria-hidden="true"
            ><RobotOutlined
          /></span>
          <SearchOutlined v-else />
        </template>
        <template v-if="showShortcutHint" #suffix>
          <kbd class="search-shortcut" aria-label="Control or Command K">Ctrl K</kbd>
        </template>
      </a-input>
      <template #option="option">
        <div v-if="option.recommendation" class="recommendation-option">
          <span
            class="recommendation-icon"
            :data-entity="option.recommendation.entityType"
            aria-hidden="true"
          >
            <component :is="recommendationIcon(option.recommendation.entityType)" />
          </span>
          <div>
            <div class="recommendation-label">{{ option.recommendation.label }}</div>
            <div class="recommendation-meta">{{ recommendationMeta(option.recommendation) }}</div>
          </div>
          <span class="recommendation-target"
            >{{ recommendationTarget(option.recommendation) }} <ArrowRightOutlined
          /></span>
        </div>
        <div v-else-if="option.kind === 'viewAll'" class="recommendation-view-all">
          <SearchOutlined aria-hidden="true" />
          <span>{{ option.label }}</span>
          <ArrowRightOutlined aria-hidden="true" />
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
.activity-search-autocomplete-shell.is-ai :deep(.ant-input-affix-wrapper) {
  min-height: 52px;
  padding: 6px 8px 6px 10px;
  background: transparent;
  border: 0;
  box-shadow: none;
}
.activity-search-autocomplete-shell.is-ai :deep(.ant-input) {
  color: var(--tm-text);
  font-size: 15px;
  line-height: 1.5;
}
.activity-search-autocomplete-shell.is-ai :deep(.ant-input::placeholder) {
  color: var(--tm-text-soft);
}
.activity-search-autocomplete-shell.is-ai.is-compact :deep(.ant-input-affix-wrapper) {
  min-height: 37px;
  padding: 3px 5px 3px 7px;
}
.activity-search-autocomplete-shell.is-ai.is-compact :deep(.ant-input) {
  min-width: 0;
  font-size: 14px;
}
.activity-search-autocomplete-shell.is-ai.is-compact .search-ai-mark {
  width: 27px;
  height: 27px;
  margin-right: 4px;
  font-size: 14px;
  border-radius: 8px;
  box-shadow: 0 3px 9px rgba(79, 70, 229, 0.18);
}
.activity-search-autocomplete-shell.is-ai.is-compact .search-shortcut {
  min-width: 34px;
  padding-inline: 4px;
}
.search-ai-mark {
  display: grid;
  width: 34px;
  height: 34px;
  margin-right: 7px;
  color: #fff;
  font-size: 16px;
  background: var(--tm-ai-grad);
  border-radius: 11px;
  box-shadow: 0 6px 16px rgba(79, 70, 229, 0.24);
  place-items: center;
}
.search-shortcut {
  min-width: 38px;
  padding: 4px 6px;
  color: var(--tm-text-soft);
  font-family: var(--tm-mono);
  font-size: 10px;
  line-height: 1;
  text-align: center;
  background: var(--tm-surface-subtle);
  border: 1px solid var(--tm-border-soft);
  border-radius: 6px;
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
.recommendation-option > div {
  flex: 1;
  min-width: 0;
}
.recommendation-icon {
  display: grid;
  flex: 0 0 34px;
  width: 34px;
  height: 34px;
  color: var(--tm-primary);
  background: var(--tm-primary-soft);
  border-radius: 10px;
  place-items: center;
}
.recommendation-icon[data-entity='project'] {
  color: var(--tm-accent-teal);
  background: color-mix(in srgb, var(--tm-accent-teal) 11%, transparent);
}

.recommendation-label,
.recommendation-view-all {
  color: var(--tm-text);
  font-weight: 600;
}

.recommendation-view-all {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  border-top: 1px solid var(--tm-border-soft);
}

.recommendation-meta,
.recommendation-target {
  color: var(--tm-text-muted);
  font-size: 12px;
}

.recommendation-target {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  white-space: nowrap;
}
:global(.activity-search-popup) {
  padding: 7px;
  background: var(--tm-card-bg);
  border: 1px solid var(--tm-border);
  border-radius: 14px;
  box-shadow: var(--tm-shadow-lg);
}
:global(.activity-search-popup .ant-select-item) {
  min-height: 48px;
  padding: 7px 8px;
  border-radius: 10px;
}
:global(
  .activity-search-popup .ant-select-item-option-active:not(.ant-select-item-option-disabled)
),
:global(
  .activity-search-popup .ant-select-item-option-selected:not(.ant-select-item-option-disabled)
) {
  background: var(--tm-primary-soft);
}
:global(.activity-search-popup--ai) {
  margin-top: 8px;
  border-color: var(--tm-primary-soft-border);
}
@media (max-width: 640px) {
  .activity-search-autocomplete-shell.is-ai :deep(.ant-input-affix-wrapper) {
    min-height: 48px;
  }
  .activity-search-autocomplete-shell.is-ai :deep(.ant-input) {
    font-size: 14px;
  }
  .activity-search-autocomplete-shell.is-ai.is-compact :deep(.ant-input-affix-wrapper) {
    min-height: 44px;
  }
  .search-shortcut,
  .recommendation-target {
    display: none;
  }
}
</style>
