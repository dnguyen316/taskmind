<script setup lang="ts">
import { ref } from 'vue'

const isOpen = ref(false)

const quickPrompts = ['What\'s blocking Atlas?', 'Plan my Monday', 'Find overdue items', 'Summarize Nova project']

const toggleWidget = () => {
  isOpen.value = !isOpen.value
}

const closeWidget = () => {
  isOpen.value = false
}
</script>

<template>
  <div class="ai-assistant" :class="{ 'ai-assistant--open': isOpen }">
    <button
      v-if="!isOpen"
      class="chat-fab"
      type="button"
      aria-label="Open AI assistant"
      @click="toggleWidget"
    >
      ✦
    </button>

    <section v-else class="chat-dialog" aria-label="AI assistant dialog">
      <div class="chat-header">
        <div class="chat-brand">
          <span class="chat-brand-icon">✦</span>
          <div>
            <strong>Nova AI</strong>
            <p>Your project assistant</p>
          </div>
        </div>
        <button class="chat-close" type="button" aria-label="Close AI assistant" @click="closeWidget">×</button>
      </div>

      <p>Hi Alex — I'm Nova. I can summarize work, find blockers, and rebalance the team.</p>
      <div class="chat-chip-row">
        <button v-for="prompt in quickPrompts" :key="prompt" type="button">{{ prompt }}</button>
      </div>
      <div class="chat-input-row">
        <div class="chat-input">Find my task</div>
        <button class="chat-send" type="button" aria-label="Send message">➤</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.ai-assistant { position: fixed; right: 20px; bottom: 20px; z-index: 1200; }
.chat-fab{width:58px;height:58px;border:none;border-radius:50%;background:linear-gradient(145deg,#2563eb,#6d28d9);color:#fff;font-size:22px;box-shadow:0 10px 30px rgba(67,56,202,.35);cursor:pointer}
.chat-dialog{width:min(380px,calc(100vw - 24px));background:#fff;border:1px solid #e2e8f0;border-radius:24px;overflow:hidden;box-shadow:0 20px 40px rgba(15,23,42,.18)}
.chat-header{background:linear-gradient(100deg,#1d4ed8,#6d28d9);color:#fff;padding:16px;display:flex;justify-content:space-between;align-items:center}
.chat-brand{display:flex;gap:10px;align-items:center}.chat-brand-icon{width:40px;height:40px;border-radius:14px;background:rgba(255,255,255,.18);display:grid;place-items:center}.chat-brand p{margin:0;color:rgba(255,255,255,.8);padding:0}
.chat-close{border:none;background:rgba(255,255,255,.18);color:#fff;border-radius:12px;width:40px;height:40px;font-size:22px;cursor:pointer}
.chat-dialog p{padding:16px;margin:0;color:#334155}.chat-chip-row{padding:0 14px 14px;display:flex;flex-wrap:wrap;gap:8px}.chat-chip-row button{border:1px solid #e2e8f0;background:#fff;border-radius:999px;padding:7px 12px;font-size:12px;cursor:pointer}
.chat-input-row{display:flex;gap:10px;padding:0 14px 14px}.chat-input{flex:1;border:1px solid #cbd5e1;border-radius:14px;padding:12px;color:#111827;background:#fff}.chat-send{border:none;border-radius:14px;width:52px;background:linear-gradient(145deg,#2563eb,#6d28d9);color:#fff;font-size:20px;cursor:pointer}
</style>
