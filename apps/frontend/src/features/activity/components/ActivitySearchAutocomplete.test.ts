import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const autocompleteSource = readFileSync(
  new URL('./ActivitySearchAutocomplete.vue', import.meta.url),
  'utf8',
)
const dashboardSource = readFileSync(
  new URL('../../tasks/pages/DashboardPage.vue', import.meta.url),
  'utf8',
)
const panelSource = readFileSync(new URL('./ActivitySearchPanel.vue', import.meta.url), 'utf8')

describe('ActivitySearchAutocomplete presentations', () => {
  it('provides an opt-in compact modifier with desktop and touch-target sizing', () => {
    expect(autocompleteSource).toContain("presentation?: 'standard' | 'compact'")
    expect(autocompleteSource).toContain("presentation: 'standard'")
    expect(autocompleteSource).toContain("'is-compact': presentation === 'compact'")
    expect(autocompleteSource).toMatch(
      /is-ai\.is-compact :deep\(\.ant-input-affix-wrapper\)[\s\S]*?min-height: 37px/,
    )
    expect(autocompleteSource).toMatch(
      /@media \(max-width: 640px\)[\s\S]*?is-ai\.is-compact :deep\(\.ant-input-affix-wrapper\)[\s\S]*?min-height: 44px/,
    )
  })

  it('uses compact presentation only for the dashboard header search', () => {
    expect(dashboardSource).toMatch(
      /<ActivitySearchAutocomplete[\s\S]*?appearance="ai"[\s\S]*?presentation="compact"/,
    )
    expect(panelSource).not.toContain('presentation="compact"')
  })
})

describe('Activity search failure handling', () => {
  it('shows a coalesced, user-facing toast when Nova submission fails', () => {
    expect(dashboardSource).toContain("import { message } from 'ant-design-vue'")
    expect(dashboardSource).toContain('key: NOVA_SEARCH_ERROR_KEY')
    expect(dashboardSource).toContain('Nova couldn’t prepare this search. Try again.')
    expect(dashboardSource).not.toContain('aiSearchErrorMessage')
    expect(dashboardSource).not.toContain('ai-search-error')
  })

  it('emits recommendation failures only for the current request', () => {
    expect(autocompleteSource).toContain('recommendationError: [message: string]')
    expect(autocompleteSource).toMatch(
      /catch \{\s+if \(requestId === suggestionRequestId\) \{[\s\S]*?emit\(\s*'recommendationError'/,
    )
    expect(autocompleteSource).toContain('suggestionRequestId += 1')
  })

  it('does not add recommendation errors to the dropdown', () => {
    expect(autocompleteSource).not.toContain('suggestionsErrorMessage')
    expect(autocompleteSource).not.toContain('`${STATUS_OPTION_PREFIX}error`')
    expect(autocompleteSource).not.toContain("return 'error'")
  })

  it('retains normal and view-all search actions after recommendation failure', () => {
    expect(autocompleteSource).toMatch(
      /catch \{[\s\S]*?suggestions\.value = \[\][\s\S]*?recommendationError/,
    )
    expect(autocompleteSource).toContain("emit('submitSearch', query)")
    expect(autocompleteSource).toContain(
      "emit('viewAll', value.slice(VIEW_ALL_OPTION_PREFIX.length))",
    )
    expect(autocompleteSource).toMatch(/if \(!props\.showViewAll\) return stateOptions/)
  })

  it('coalesces recommendation failure toasts in each owning page', () => {
    expect(dashboardSource).toContain('key: RECOMMENDATION_ERROR_KEY')
    expect(panelSource).toContain('key: RECOMMENDATION_ERROR_KEY')
    expect(dashboardSource).toContain('@recommendation-error="showRecommendationError"')
    expect(panelSource).toContain('@recommendation-error="showRecommendationError"')
  })
})
