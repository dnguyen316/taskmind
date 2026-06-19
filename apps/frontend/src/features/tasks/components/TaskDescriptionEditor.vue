<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'

const description = defineModel<string>({ required: true })
const editorRef = ref<HTMLTextAreaElement | null>(null)

onMounted(() => {
  resizeEditor()
})

watch(description, () => {
  void nextTick(resizeEditor)
})

function resizeEditor() {
  const editor = editorRef.value
  if (!editor) {
    return
  }

  editor.style.height = 'auto'
  editor.style.height = `${Math.max(editor.scrollHeight, 220)}px`
}
</script>

<template>
  <div class="task-description-editor">
    <textarea
      ref="editorRef"
      v-model="description"
      class="task-description-editor__input"
      placeholder="Write notes, context, decisions, or next steps…"
      aria-label="Task description"
      @input="resizeEditor"
    />
  </div>
</template>

<style scoped>
.task-description-editor {
  border: 1px solid color-mix(in srgb, var(--tm-border) 70%, transparent);
  border-radius: 22px;
  background: var(--tm-surface);
  box-shadow: 0 18px 45px color-mix(in srgb, var(--tm-text) 6%, transparent);
  transition:
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    background-color 0.18s ease;
}

.task-description-editor:focus-within {
  border-color: var(--tm-border);
  box-shadow:
    0 20px 55px color-mix(in srgb, var(--tm-text) 8%, transparent),
    0 0 0 3px color-mix(in srgb, var(--tm-primary, #646cff) 16%, transparent);
}

.task-description-editor__input {
  display: block;
  width: 100%;
  min-height: 220px;
  padding: 28px 30px;
  resize: none;
  overflow: hidden;
  color: var(--tm-text);
  background: transparent;
  border: 0;
  outline: none;
  font: inherit;
  font-size: 1.08rem;
  line-height: 1.75;
  letter-spacing: -0.01em;
  caret-color: var(--tm-primary, #646cff);
}

.task-description-editor__input::placeholder {
  color: var(--tm-text-muted);
}

@media (max-width: 640px) {
  .task-description-editor {
    border-radius: 18px;
  }

  .task-description-editor__input {
    min-height: 180px;
    padding: 22px 20px;
    font-size: 1rem;
  }
}
</style>
