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
