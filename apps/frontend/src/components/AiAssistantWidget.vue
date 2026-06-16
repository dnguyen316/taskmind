<script setup lang="ts">
import { ref } from 'vue'
import { CloseOutlined, SendOutlined } from '@ant-design/icons-vue'
import { useNovaChat } from '../features/ai/composables/useNovaChat'

const text = ref('')
const open = ref(false)
const { messages, loading, errorMessage, send } = useNovaChat()

async function submit() {
  if (text.value.trim()) {
    const message = text.value
    text.value = ''
    await send(message)
  }
}
</script>

<template>
  <div class="ai-widget">
    <a-card v-if="open" class="ai-panel" :bordered="false">
      <template #title>
        <div class="ai-panel__header">
          <div>
            <span>TaskMind AI</span>
            <p>Ask about your tasks</p>
          </div>
          <a-button
            aria-label="Close Nova assistant"
            shape="circle"
            size="small"
            type="text"
            @click="open = false"
          >
            <template #icon><CloseOutlined /></template>
          </a-button>
        </div>
      </template>

      <div class="messages" aria-live="polite">
        <p v-if="messages.length === 0" class="empty-state">Ask Nova about your tasks.</p>
        <div
          v-for="(message, index) in messages"
          :key="index"
          class="message-row"
          :class="`message-row--${message.role}`"
        >
          <div class="message-bubble" :class="`message-bubble--${message.role}`">
            <span class="message-label">{{ message.role === 'user' ? 'You' : 'Nova' }}</span>
            <p>{{ message.content }}</p>
          </div>
        </div>
      </div>

      <a-alert
        v-if="errorMessage"
        class="ai-error"
        type="error"
        :message="errorMessage"
        show-icon
      />

      <div class="ai-composer">
        <a-input
          v-model:value="text"
          aria-label="Ask Nova about your tasks"
          placeholder="Ask TaskMind AI..."
          :disabled="loading"
          @press-enter="submit"
        />
        <a-button
          aria-label="Send message to TaskMind AI"
          shape="circle"
          type="primary"
          :loading="loading"
          @click="submit"
        >
          <template #icon><SendOutlined /></template>
        </a-button>
      </div>
    </a-card>
    <a-button
      class="ai-launcher"
      aria-label="Open TaskMind AI assistant"
      shape="circle"
      type="primary"
      @click="open = !open"
    >
      AI
    </a-button>
  </div>
</template>

<style scoped>
.ai-widget {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 20;
}

.ai-panel {
  width: min(380px, calc(100vw - 32px));
  margin-bottom: 14px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(79, 70, 229, 0.16);
  border-radius: 24px;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.22);
  backdrop-filter: blur(18px);
}

.ai-panel :deep(.ant-card-head) {
  min-height: 66px;
  padding: 0 18px;
  color: #fff;
  background: linear-gradient(135deg, #2f256f, #4f46e5 58%, #7c3aed);
  border-bottom: 0;
}

.ai-panel :deep(.ant-card-head-title) {
  padding: 14px 0;
}

.ai-panel :deep(.ant-card-body) {
  display: grid;
  gap: 14px;
  padding: 16px;
  background: linear-gradient(180deg, rgba(244, 247, 255, 0.95), #fff);
}

.ai-panel__header {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.ai-panel__header span {
  display: block;
  font-size: 15px;
  font-weight: 800;
  letter-spacing: -0.02em;
}

.ai-panel__header p {
  margin: 2px 0 0;
  color: rgba(255, 255, 255, 0.74);
  font-size: 12px;
  font-weight: 500;
}

.ai-panel__header .ant-btn {
  color: rgba(255, 255, 255, 0.9);
}

.messages {
  display: grid;
  gap: 10px;
  max-height: 300px;
  overflow: auto;
  padding: 2px;
}

.empty-state {
  margin: 0;
  padding: 16px;
  color: var(--tm-text-muted);
  font-size: 13px;
  text-align: center;
  background: rgba(255, 255, 255, 0.78);
  border: 1px dashed rgba(79, 70, 229, 0.22);
  border-radius: 18px;
}

.message-row {
  display: flex;
}

.message-row--user {
  justify-content: flex-end;
}

.message-row--assistant {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 84%;
  padding: 10px 12px;
  line-height: 1.45;
  border-radius: 18px;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.08);
}

.message-bubble--user {
  color: #fff;
  background: linear-gradient(135deg, #4338ca, #6d5dfc);
  border-bottom-right-radius: 6px;
}

.message-bubble--assistant {
  color: var(--tm-text);
  background: #fff;
  border: 1px solid var(--tm-border-soft);
  border-bottom-left-radius: 6px;
}

.message-label {
  display: block;
  margin-bottom: 4px;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  opacity: 0.72;
}

.message-bubble p {
  margin: 0;
}

.ai-error {
  margin: 0;
}

.ai-composer {
  display: flex;
  gap: 8px;
  align-items: center;
  padding: 8px;
  background: #fff;
  border: 1px solid var(--tm-border);
  border-radius: 999px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
}

.ai-composer :deep(.ant-input) {
  border: 0;
  box-shadow: none;
}

.ai-composer .ant-btn-primary,
.ai-launcher.ant-btn-primary {
  background: linear-gradient(135deg, #2f256f, #4f46e5 58%, #7c3aed);
  border: 0;
  box-shadow: 0 12px 28px rgba(79, 70, 229, 0.35);
}

.ai-launcher {
  display: grid;
  width: 44px;
  height: 44px;
  margin-left: auto;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: -0.02em;
  place-items: center;
}
</style>
