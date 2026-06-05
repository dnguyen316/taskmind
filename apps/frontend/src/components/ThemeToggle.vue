<script setup lang="ts">
import { BulbOutlined, StarOutlined } from '@ant-design/icons-vue'
import { useTheme } from '../composables/useTheme'

const { isDark, toggleTheme } = useTheme()
</script>

<template>
  <button
    class="theme-toggle"
    type="button"
    :aria-label="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
    :title="isDark ? 'Switch to light mode' : 'Switch to dark mode'"
    @click="toggleTheme"
  >
    <BulbOutlined v-if="isDark" />
    <StarOutlined v-else />
  </button>
</template>

<style scoped>
.theme-toggle {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid var(--tm-border);
  border-radius: 10px;
  color: var(--tm-text-muted);
  background: var(--tm-surface);
  box-shadow: var(--tm-shadow-sm);
  cursor: pointer;
  transition: color 160ms ease, border-color 160ms ease, background 160ms ease, transform 160ms ease;
}

.theme-toggle:hover {
  color: var(--tm-primary);
  border-color: var(--tm-primary-soft-border);
  background: var(--tm-primary-soft);
  transform: translateY(-1px);
}
</style>
