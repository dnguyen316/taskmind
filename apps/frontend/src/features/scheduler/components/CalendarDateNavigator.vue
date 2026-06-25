<script setup lang="ts">
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'

defineProps<{ title: string; loading?: boolean }>()
const emit = defineEmits<{ today: []; previous: []; next: [] }>()
</script>

<template>
  <div class="calendar-date-navigator">
    <a-button size="large" @click="emit('today')">Today</a-button>
    <a-button-group>
      <a-button size="large" aria-label="Previous period" @click="emit('previous')">
        <template #icon><LeftOutlined /></template>
      </a-button>
      <a-button size="large" aria-label="Next period" @click="emit('next')">
        <template #icon><RightOutlined /></template>
      </a-button>
    </a-button-group>
    <h2>{{ title }}</h2>
    <a-spin v-if="loading" size="small" />
  </div>
</template>

<style scoped>
.calendar-date-navigator {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
h2 {
  color: #0f172a;
  font-size: 22px;
  margin: 0;
}
</style>
