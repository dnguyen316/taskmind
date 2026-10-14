import type { ChainedCommands } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import TaskList from '@tiptap/extension-task-list'
import TaskItem from '@tiptap/extension-task-item'
import { Markdown } from '@tiptap/markdown'

export function createTaskEditorExtensions() {
  return [
    StarterKit.configure({ heading: { levels: [1, 2, 3] }, link: { openOnClick: false } }),
    Placeholder.configure({ placeholder: 'Type / for commands', showOnlyCurrent: true }),
    TaskList,
    TaskItem.configure({ nested: true }),
    Markdown.configure({ markedOptions: { breaks: true } }),
  ]
}
export interface TaskBlockCommand {
  id: string
  label: string
  icon: string
  keywords: string
  run: (chain: ChainedCommands) => ChainedCommands
}
export const TASK_BLOCK_COMMANDS: TaskBlockCommand[] = [
  {
    id: 'text',
    label: 'Text',
    icon: 'T',
    keywords: 'paragraph plain',
    run: (chain) => chain.setParagraph(),
  },
  {
    id: 'heading1',
    label: 'Heading 1',
    icon: 'H₁',
    keywords: 'h1 heading1 title large',
    run: (chain) => chain.setHeading({ level: 1 }),
  },
  {
    id: 'heading2',
    label: 'Heading 2',
    icon: 'H₂',
    keywords: 'h2 heading2 subtitle medium',
    run: (chain) => chain.setHeading({ level: 2 }),
  },
  {
    id: 'heading3',
    label: 'Heading 3',
    icon: 'H₃',
    keywords: 'h3 heading3 subtitle small',
    run: (chain) => chain.setHeading({ level: 3 }),
  },
  {
    id: 'bullet',
    label: 'Bulleted list',
    icon: '•',
    keywords: 'unordered bullet',
    run: (chain) => chain.toggleBulletList(),
  },
  {
    id: 'numbered',
    label: 'Numbered list',
    icon: '1.',
    keywords: 'ordered number',
    run: (chain) => chain.toggleOrderedList(),
  },
  {
    id: 'todo',
    label: 'To-do list',
    icon: '☑',
    keywords: 'todo to do checklist checkbox task',
    run: (chain) => chain.toggleTaskList(),
  },
  {
    id: 'quote',
    label: 'Quote',
    icon: '❞',
    keywords: 'blockquote',
    run: (chain) => chain.toggleBlockquote(),
  },
  {
    id: 'code',
    label: 'Code',
    icon: '</>',
    keywords: 'codeblock snippet',
    run: (chain) => chain.toggleCodeBlock(),
  },
  {
    id: 'divider',
    label: 'Divider',
    icon: '—',
    keywords: 'horizontal rule line',
    run: (chain) => chain.setHorizontalRule(),
  },
]
export function matchSlashCommand(text: string): { query: string } | null {
  const match = /^\/([\p{L}\p{N} -]*)$/u.exec(text)
  return match ? { query: match[1] ?? '' } : null
}
