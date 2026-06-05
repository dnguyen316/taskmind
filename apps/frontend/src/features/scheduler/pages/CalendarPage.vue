<script setup lang="ts">
import { CalendarOutlined, ClockCircleOutlined } from '@ant-design/icons-vue'
import AppLayout from '../../tasks/components/AppLayout.vue'

const previewBlocks = [
  { label: 'Focus block', time: '09:00–10:30', note: 'Auto-scheduled from task priority in M04' },
  {
    label: 'Project review',
    time: '13:00–14:00',
    note: 'Reschedule proposals arrive with scheduler conflicts',
  },
  { label: 'Nova rationale', time: 'M08', note: 'AI duration and rationale phrase wiring' },
]
</script>

<template>
  <AppLayout>
    <section class="placeholder-page">
      <a-card class="hero-card">
        <div class="icon"><CalendarOutlined /></div>
        <a-tag color="purple">Coming in M04</a-tag>
        <h1>Calendar</h1>
        <p>
          The M04 scheduler milestone adds scheduling preferences, generated calendar blocks,
          missed-block handling, and conflict-aware reschedule proposals. Nova duration/rationale
          assists are labeled until M08.
        </p>
      </a-card>

      <a-card title="Scheduler preview">
        <div class="block-grid">
          <article v-for="block in previewBlocks" :key="block.label" class="block-card">
            <ClockCircleOutlined />
            <strong>{{ block.label }}</strong>
            <span>{{ block.time }}</span>
            <p>{{ block.note }}</p>
          </article>
        </div>
      </a-card>
    </section>
  </AppLayout>
</template>

<style scoped>
.placeholder-page {
  display: grid;
  gap: 16px;
}
.hero-card {
  border: 1px solid #ddd6fe;
  border-radius: 16px;
}
.icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: #f5f3ff;
  color: #7c3aed;
  font-size: 24px;
  margin-bottom: 12px;
}
h1 {
  margin: 10px 0 8px;
}
p {
  color: #475569;
  max-width: 760px;
}
.block-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}
.block-card {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 16px;
  display: grid;
  gap: 8px;
}
.block-card span {
  color: #7c3aed;
  font-weight: 600;
}
.block-card p {
  margin: 0;
}
@media (max-width: 900px) {
  .block-grid {
    grid-template-columns: 1fr;
  }
}
</style>
