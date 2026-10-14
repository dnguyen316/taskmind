<script setup lang="ts">
import { computed, nextTick, ref, useId, watch } from 'vue'
import { EditorContent, useEditor } from '@tiptap/vue-3'
import {
  BoldOutlined,
  ItalicOutlined,
  StrikethroughOutlined,
  CodeOutlined,
  PlusOutlined,
  MoreOutlined,
  UnorderedListOutlined,
  OrderedListOutlined,
  CheckSquareOutlined,
  MinusOutlined,
} from '@ant-design/icons-vue'
import {
  createTaskEditorExtensions,
  TASK_BLOCK_COMMANDS,
  matchSlashCommand,
} from '../editor/taskEditor'
import type { TaskBlockCommand } from '../editor/taskEditor'
import { TASK_DESCRIPTION_MAX_LENGTH } from '../validation/taskFormValidation'

const description = defineModel<string>({ required: true })
const props = withDefaults(defineProps<{ disabled?: boolean }>(), { disabled: false })
const root = ref<HTMLElement | null>(null)
const menuId = useId()
const activeCommand = ref(0)
const slash = ref<{ from: number; to: number; query: string } | null>(null)
const dismissedSlash = ref<number | null>(null)
const selectionVisible = ref(false)
const blockMenuOpen = ref(false)
const blockRange = ref<{ from: number; to: number } | null>(null)
const menuPosition = ref({ top: 0, left: 0 })
const toolbarPosition = ref({ top: 0, left: 0 })
const blockTop = ref(0)
let lastEmitted = description.value

const commands = computed(() => {
  const query = slash.value?.query.toLowerCase() ?? ''
  return TASK_BLOCK_COMMANDS.filter((command) =>
    `${command.label} ${command.keywords}`.toLowerCase().includes(query),
  )
})
const overLimit = computed(() => description.value.length > TASK_DESCRIPTION_MAX_LENGTH)
const commandIcons = {
  bullet: UnorderedListOutlined,
  numbered: OrderedListOutlined,
  todo: CheckSquareOutlined,
  code: CodeOutlined,
  divider: MinusOutlined,
}
function commandIcon(id: string) {
  return commandIcons[id as keyof typeof commandIcons]
}
const editor = useEditor({
  extensions: createTaskEditorExtensions(),
  content: description.value,
  contentType: 'markdown',
  editable: !props.disabled,
  editorProps: {
    attributes: {
      role: 'textbox',
      'aria-label': 'Task description',
      'aria-multiline': 'true',
      spellcheck: 'true',
    },
    handleKeyDown: (_view, event) => {
      if (!slash.value || event.isComposing) return false
      if (event.key === 'Escape') {
        dismissedSlash.value = slash.value.from
        slash.value = null
        return true
      }
      if (commands.value.length && (event.key === 'ArrowDown' || event.key === 'ArrowUp')) {
        const direction = event.key === 'ArrowDown' ? 1 : -1
        activeCommand.value =
          (activeCommand.value + direction + commands.value.length) % commands.value.length
        void nextTick(() =>
          document
            .getElementById(`${menuId}-${activeCommand.value}`)
            ?.scrollIntoView({ block: 'nearest' }),
        )
        return true
      }
      if (event.key === 'Enter' && commands.value.length) {
        const command = commands.value[activeCommand.value]
        if (command) runCommand(command)
        return true
      }
      return false
    },
  },
  onUpdate: ({ editor: instance }) => {
    lastEmitted = instance.getMarkdown()
    description.value = lastEmitted
    updateMenus()
  },
  onSelectionUpdate: () => {
    blockMenuOpen.value = false
    updateMenus()
  },
  onFocus: () => updateMenus(),
  onBlur: () => {
    slash.value = null
    selectionVisible.value = false
  },
})
watch(description, (value) => {
  if (value === lastEmitted) return
  lastEmitted = value
  editor.value?.commands.setContent(value, { contentType: 'markdown', emitUpdate: false })
  slash.value = null
  selectionVisible.value = false
})
watch(
  () => props.disabled,
  (disabled) => editor.value?.setEditable(!disabled, false),
)
watch(
  () => slash.value?.query,
  () => {
    activeCommand.value = 0
  },
)
watch([editor, slash, activeCommand, overLimit], () => {
  const element = editor.value?.view.dom
  if (!element) return
  element.setAttribute('aria-describedby', `${menuId}-help`)
  element.setAttribute('aria-invalid', String(overLimit.value))
  if (slash.value) {
    element.setAttribute('aria-controls', menuId)
    element.setAttribute('aria-expanded', 'true')
    if (commands.value.length)
      element.setAttribute('aria-activedescendant', `${menuId}-${activeCommand.value}`)
    else element.removeAttribute('aria-activedescendant')
  } else {
    element.removeAttribute('aria-controls')
    element.removeAttribute('aria-activedescendant')
    element.setAttribute('aria-expanded', 'false')
  }
})

function updateMenus() {
  const instance = editor.value
  if (!instance || !root.value || !instance.isFocused || props.disabled) return
  const { $from, from, empty } = instance.state.selection
  const bounds = root.value.getBoundingClientRect()
  const caret = instance.view.coordsAtPos(from)
  const textBefore = $from.parent.textBetween(0, $from.parentOffset, '\n')
  const match =
    empty && $from.parent.type.name === 'paragraph' ? matchSlashCommand(textBefore) : null
  if (!match) dismissedSlash.value = null
  const start = from - textBefore.length
  slash.value =
    match && dismissedSlash.value !== start ? { from: start, to: from, query: match.query } : null
  const menuHeight = Math.min(392, window.innerHeight - 32)
  const menuTop =
    caret.bottom + menuHeight + 8 > window.innerHeight - 16
      ? Math.max(16, caret.top - menuHeight - 8)
      : caret.bottom + 8
  menuPosition.value = {
    top: menuTop - bounds.top,
    left: Math.max(0, Math.min(caret.left - bounds.left, bounds.width - 280)),
  }
  selectionVisible.value = !empty && !slash.value
  toolbarPosition.value = {
    top: Math.max(0, caret.top - bounds.top - 42),
    left: Math.max(0, Math.min(caret.left - bounds.left, bounds.width - 176)),
  }
  if ($from.depth > 0) {
    const blockFrom = $from.before(1)
    const node = $from.node(1)
    blockRange.value = { from: blockFrom, to: blockFrom + node.nodeSize }
    blockTop.value = instance.view.coordsAtPos(blockFrom + 1).top - bounds.top
  }
}
function runCommand(command: TaskBlockCommand) {
  const instance = editor.value
  if (!instance || props.disabled) return
  const chain = instance.chain().focus()
  if (slash.value) chain.deleteRange({ from: slash.value.from, to: slash.value.to })
  command.run(chain).run()
  slash.value = null
  dismissedSlash.value = null
}
function addBlock() {
  const instance = editor.value
  if (!instance || props.disabled) return
  instance
    .chain()
    .focus('end')
    .insertContentAt(instance.state.doc.content.size, {
      type: 'paragraph',
      content: [{ type: 'text', text: '/' }],
    })
    .run()
  instance.commands.focus('end')
  updateMenus()
}
function duplicateBlock() {
  const instance = editor.value
  const range = blockRange.value
  if (!instance || !range) return
  const node = instance.state.doc.nodeAt(range.from)
  if (node) instance.chain().focus().insertContentAt(range.to, node.toJSON()).run()
  blockMenuOpen.value = false
}
function deleteBlock() {
  if (blockRange.value) editor.value?.chain().focus().deleteRange(blockRange.value).run()
  blockMenuOpen.value = false
}
</script>

<template>
  <div
    ref="root"
    class="task-description-editor"
    :class="{ 'is-disabled': disabled }"
    @keydown.esc="blockMenuOpen = false"
  >
    <EditorContent :editor="editor" />
    <div
      v-if="selectionVisible && editor && !disabled"
      class="format-toolbar"
      :style="{ top: `${toolbarPosition.top}px`, left: `${toolbarPosition.left}px` }"
      role="toolbar"
      aria-label="Text formatting"
      @mousedown.prevent
    >
      <button
        type="button"
        title="Bold (Ctrl/Cmd+B)"
        aria-label="Bold"
        :aria-pressed="editor.isActive('bold')"
        @click="editor.chain().focus().toggleBold().run()"
      >
        <BoldOutlined />
      </button>
      <button
        type="button"
        title="Italic (Ctrl/Cmd+I)"
        aria-label="Italic"
        :aria-pressed="editor.isActive('italic')"
        @click="editor.chain().focus().toggleItalic().run()"
      >
        <ItalicOutlined />
      </button>
      <button
        type="button"
        title="Strikethrough"
        aria-label="Strikethrough"
        :aria-pressed="editor.isActive('strike')"
        @click="editor.chain().focus().toggleStrike().run()"
      >
        <StrikethroughOutlined />
      </button>
      <button
        type="button"
        title="Inline code"
        aria-label="Inline code"
        :aria-pressed="editor.isActive('code')"
        @click="editor.chain().focus().toggleCode().run()"
      >
        <CodeOutlined />
      </button>
    </div>
    <div
      v-if="slash && !disabled"
      :id="menuId"
      class="slash-menu"
      :style="{ top: `${menuPosition.top}px`, left: `${menuPosition.left}px` }"
      role="listbox"
      aria-label="Add a block"
      @mousedown.prevent
    >
      <div class="slash-menu__label">Add a block</div>
      <button
        v-for="(command, index) in commands"
        :id="`${menuId}-${index}`"
        :key="command.id"
        type="button"
        role="option"
        :aria-selected="index === activeCommand"
        :class="{ 'is-selected': index === activeCommand }"
        @mouseenter="activeCommand = index"
        @click="runCommand(command)"
      >
        <span class="command-icon" aria-hidden="true"
          ><component :is="commandIcon(command.id)" v-if="commandIcon(command.id)" /><template
            v-else
            >{{ command.icon }}</template
          ></span
        ><span>{{ command.label }}</span>
      </button>
      <p v-if="!commands.length" class="slash-menu__empty">No matching blocks</p>
    </div>
    <div v-if="blockRange && !disabled" class="block-actions" :style="{ top: `${blockTop}px` }">
      <button
        type="button"
        class="block-handle"
        aria-label="Block actions"
        :aria-expanded="blockMenuOpen"
        @mousedown.prevent
        @click="blockMenuOpen = !blockMenuOpen"
      >
        <MoreOutlined />
      </button>
      <div v-if="blockMenuOpen" class="block-menu" @mousedown.prevent>
        <button type="button" @click="duplicateBlock">Duplicate block</button>
        <button type="button" @click="deleteBlock">Delete block</button>
      </div>
    </div>
    <div :id="`${menuId}-help`" class="editor-footer">
      <button type="button" class="add-block" :disabled="disabled" @click="addBlock">
        <PlusOutlined aria-hidden="true" /> Add block
      </button>
      <span class="editor-hint">Enter for a new block · Shift + Enter for a new line</span>
      <span class="character-count" :class="{ 'is-invalid': overLimit }" aria-live="polite"
        >{{ description.length.toLocaleString() }} /
        {{ TASK_DESCRIPTION_MAX_LENGTH.toLocaleString() }}</span
      >
    </div>
    <p v-if="overLimit" class="editor-error" role="alert">
      Description must be at most {{ TASK_DESCRIPTION_MAX_LENGTH.toLocaleString() }} characters.
      Shorten it before saving.
    </p>
  </div>
</template>

<style scoped>
.task-description-editor {
  position: relative;
  color: var(--tm-text);
}
:deep(.tiptap) {
  min-height: 480px;
  padding: 20px 0 48px;
  outline: none;
  font-size: 16px;
  line-height: 1.75;
  overflow-wrap: anywhere;
}
:deep(.tiptap > :first-child) {
  margin-top: 0;
}
:deep(.tiptap p) {
  margin: 0 0 10px;
}
:deep(.tiptap h1) {
  font-size: 30px;
}
:deep(.tiptap h2) {
  font-size: 28px;
}
:deep(.tiptap h3) {
  font-size: 20px;
}
:deep(.tiptap h1),
:deep(.tiptap h2),
:deep(.tiptap h3) {
  margin: 24px 0 10px;
  font-weight: 650;
  line-height: 1.3;
  letter-spacing: -0.025em;
}
:deep(.tiptap .is-empty::before) {
  float: left;
  height: 0;
  color: var(--tm-text-muted);
  content: attr(data-placeholder);
  pointer-events: none;
}
:deep(.tiptap ul),
:deep(.tiptap ol) {
  padding-left: 24px;
  margin: 10px 0;
}
:deep(.tiptap li > p) {
  margin: 0;
}
:deep(.tiptap ul[data-type='taskList']) {
  list-style: none;
  padding-left: 0;
}
:deep(.tiptap ul[data-type='taskList'] > li) {
  display: flex;
  gap: 10px;
  align-items: baseline;
  margin: 6px 0;
}
:deep(.tiptap ul[data-type='taskList'] > li > label) {
  flex: 0 0 auto;
  user-select: none;
}
:deep(.tiptap ul[data-type='taskList'] > li > div) {
  flex: 1;
  min-width: 0;
}
:deep(.tiptap input[type='checkbox']) {
  width: 16px;
  height: 16px;
  accent-color: var(--tm-primary);
  cursor: pointer;
}
:deep(.tiptap li[data-checked='true'] > div > p) {
  color: var(--tm-text-muted);
  text-decoration: line-through;
}
:deep(.tiptap blockquote) {
  border-left: 3px solid var(--tm-border);
  padding: 2px 0 2px 16px;
  margin: 16px 0;
}
:deep(.tiptap code) {
  background: var(--tm-surface-muted);
  border-radius: 4px;
  padding: 2px 5px;
  font-size: 0.9em;
  font-family: ui-monospace, monospace;
}
:deep(.tiptap pre) {
  background: var(--tm-surface-subtle);
  border: 1px solid var(--tm-border);
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
}
:deep(.tiptap pre code) {
  background: none;
  padding: 0;
}
:deep(.tiptap hr) {
  border: 0;
  border-top: 1px solid var(--tm-border);
  margin: 24px 0;
}
:deep(.tiptap a) {
  color: var(--tm-primary);
  text-decoration: underline;
}
.format-toolbar,
.slash-menu,
.block-menu {
  position: absolute;
  z-index: 30;
  background: var(--tm-surface);
  border: 1px solid var(--tm-border);
  border-radius: 8px;
  box-shadow: 0 8px 28px rgb(0 0 0 / 12%);
  padding: 6px;
}
.format-toolbar {
  display: flex;
  gap: 2px;
}
button {
  font: inherit;
  color: inherit;
  cursor: pointer;
}
.format-toolbar button,
.block-handle {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 32px;
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: 4px;
}
.format-toolbar button:hover,
.format-toolbar button[aria-pressed='true'],
.slash-menu .is-selected,
.block-menu button:hover {
  background: var(--tm-surface-muted);
}
button:focus-visible {
  outline: 2px solid var(--tm-primary);
  outline-offset: 2px;
}
.slash-menu {
  width: 280px;
  max-width: 100%;
  max-height: min(392px, calc(100vh - 32px));
  overflow-y: auto;
}
.slash-menu__label {
  padding: 4px 10px 8px;
  font-size: 12px;
  color: var(--tm-text-muted);
}
.slash-menu button {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 5px 10px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  text-align: left;
  font-size: 14px;
}
.command-icon {
  width: 24px;
  font-size: 15px;
  font-weight: 600;
  text-align: center;
}
.slash-menu__empty {
  margin: 8px 10px;
  font-size: 13px;
  color: var(--tm-text-muted);
}
.block-actions {
  position: absolute;
  left: -36px;
  z-index: 25;
}
.block-handle {
  opacity: 0;
  color: var(--tm-text-muted);
}
.task-description-editor:hover .block-handle,
.block-handle:focus-visible,
.block-handle[aria-expanded='true'] {
  opacity: 1;
}
.block-menu {
  width: 170px;
  left: 0;
  top: 34px;
}
.block-menu button {
  display: block;
  width: 100%;
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: 4px;
  padding: 8px;
  font-size: 13px;
}
.editor-footer {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-top: 1px solid var(--tm-border);
  color: var(--tm-text-muted);
  font-size: 12px;
}
.add-block {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 0;
  background: none;
  border: 0;
  font-size: 12px;
  white-space: nowrap;
}
.add-block:hover {
  color: var(--tm-text);
}
.character-count {
  margin-left: auto;
  white-space: nowrap;
}
.is-invalid,
.editor-error {
  color: var(--tm-accent-red, #dc2626);
}
.editor-error {
  margin-top: 8px;
  font-size: 13px;
}
.is-disabled {
  opacity: 0.7;
}
@media (max-width: 640px) {
  :deep(.tiptap) {
    min-height: 260px;
    font-size: 16px;
  }
  .editor-hint {
    display: none;
  }
  .block-actions {
    left: -24px;
  }
  .block-handle {
    width: 24px;
    opacity: 1;
  }
  .slash-menu button {
    min-height: 40px;
  }
}
</style>
