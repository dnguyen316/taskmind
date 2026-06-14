<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useNovaChat } from '../features/ai/composables/useNovaChat'

const route = useRoute()
const text = ref('')
const open = ref(false)
const { messages, loading, send } = useNovaChat()
const hidden = computed(() => String(route.path).startsWith('/auth') || String(route.path) === '/')
async function submit() {
  if (text.value.trim()) {
    const message = text.value
    text.value = ''
    await send(message)
  }
}
</script>

<template>
  <div v-if="!hidden" class="ai-widget">
    <a-button shape="circle" type="primary" @click="open = !open">AI</a-button>
    <a-card v-if="open" title="Nova" class="ai-panel">
      <div class="messages">
        <p v-for="(message, index) in messages" :key="index">
          <strong>{{ message.role }}:</strong> {{ message.content }}
        </p>
      </div>
      <a-input-search
        v-model:value="text"
        enter-button="Send"
        :loading="loading"
        @search="submit"
      />
    </a-card>
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
.messages {
  max-height: 260px;
  overflow: auto;
}
</style>
