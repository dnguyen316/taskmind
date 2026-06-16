<script setup lang="ts">
import { ref } from 'vue'
import { CloseOutlined } from '@ant-design/icons-vue'
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
    <a-card v-if="open" class="ai-panel">
      <template #title>
        <div class="ai-panel__header">
          <span>Nova</span>
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

      <a-input-search
        v-model:value="text"
        aria-label="Ask Nova about your tasks"
        enter-button="Send"
        :loading="loading"
        @search="submit"
      />
    </a-card>
    <a-button aria-label="Open Nova assistant" shape="circle" type="primary" @click="open = !open">
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
  width: 320px;
  margin-bottom: 12px;
}
.ai-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.messages {
  max-height: 260px;
  overflow: auto;
}
.empty-state {
  margin: 0 0 16px;
  color: rgba(0, 0, 0, 0.45);
}
.message-row {
  display: flex;
  margin-bottom: 12px;
}
.message-row--user {
  justify-content: flex-end;
}
.message-row--assistant {
  justify-content: flex-start;
}
.message-bubble {
  max-width: 84%;
  border-radius: 14px;
  padding: 10px 12px;
  line-height: 1.45;
}
.message-bubble--user {
  background: #1677ff;
  color: #fff;
}
.message-bubble--assistant {
  background: #f5f5f5;
  color: rgba(0, 0, 0, 0.88);
}
.message-label {
  display: block;
  margin-bottom: 4px;
  font-size: 12px;
  font-weight: 600;
  opacity: 0.75;
}
.message-bubble p {
  margin: 0;
}
.ai-error {
  margin-bottom: 12px;
}
</style>
