import { describe, expect, it } from 'vitest'
import { MarkdownManager } from '@tiptap/markdown'
import { createTaskEditorExtensions, matchSlashCommand } from './taskEditor'

const markdown = new MarkdownManager({
  extensions: createTaskEditorExtensions(),
  markedOptions: { breaks: true },
})

describe('task document persistence', () => {
  it('loads existing plain-text descriptions', () => {
    const doc = markdown.parse('Notes for the launch.\n\nKeep the rollout small.')
    expect(doc.content?.map((node) => node.type)).toEqual(['paragraph', 'paragraph'])
    expect(markdown.serialize(doc)).toBe('Notes for the launch.\n\nKeep the rollout small.')
  })
  it('preserves single line breaks in existing descriptions', () => {
    const doc = markdown.parse('First line\nSecond line')
    expect(doc.content?.[0]?.content?.map((node) => node.type)).toEqual([
      'text',
      'hardBreak',
      'text',
    ])
    expect(markdown.parse(markdown.serialize(doc))).toEqual(doc)
  })
  it('round-trips headings, formatting, lists, checked items, quotes and code', () => {
    const text =
      '# Launch\n\n**Bold** and *italic* and `code`\n\n- [ ] Pending\n- [x] Done\n\n- Release notes\n\n1. Deploy\n2. Monitor\n\n> Small rollout\n\n```\nconst ready = true\n```\n\n---'
    const doc = markdown.parse(text)
    const saved = markdown.serialize(doc)
    expect(markdown.parse(saved)).toEqual(doc)
    expect(saved).toContain('- [x] Done')
    expect(saved).toContain('# Launch')
    expect(saved).toContain('**Bold**')
  })
  it('serializes an empty document to an empty string', () => {
    expect(markdown.serialize(markdown.parse(''))).toBe('')
  })
})
describe('slash commands', () => {
  it('matches a slash only at the start of a block', () => {
    expect(matchSlashCommand('/')).toEqual({ query: '' })
    expect(matchSlashCommand('/heading 2')).toEqual({ query: 'heading 2' })
    expect(matchSlashCommand('https://taskmind.app/')).toBeNull()
    expect(matchSlashCommand('notes /todo')).toBeNull()
    expect(matchSlashCommand('/todo\nnotes')).toBeNull()
  })
})
