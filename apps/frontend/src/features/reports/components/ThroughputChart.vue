<script setup lang="ts">
import { computed } from 'vue'
import type { ReportsTrend } from '../types'

const props = defineProps<{ trends: ReportsTrend[] }>()

const maxValue = computed(() =>
  Math.max(1, ...props.trends.flatMap((trend) => [trend.tasksCreated, trend.tasksCompleted])),
)
</script>

<template>
  <div class="throughput-chart" aria-label="Created and completed task throughput by day">
    <div v-if="trends.length === 0" class="empty-chart">No throughput activity yet.</div>
    <div v-for="trend in trends" v-else :key="trend.date" class="trend-row">
      <span class="trend-date">{{ trend.date }}</span>
      <div class="bar-stack">
        <span
          class="bar created"
          :style="{ width: `${Math.max(4, (trend.tasksCreated / maxValue) * 100)}%` }"
        >
          Created {{ trend.tasksCreated }}
        </span>
        <span
          class="bar completed"
          :style="{ width: `${Math.max(4, (trend.tasksCompleted / maxValue) * 100)}%` }"
        >
          Completed {{ trend.tasksCompleted }}
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.throughput-chart {
  display: grid;
  gap: 12px;
}
.trend-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}
.trend-date {
  color: var(--tm-text-muted);
  font-size: 12px;
}
.bar-stack {
  display: grid;
  gap: 4px;
}
.bar {
  min-width: 54px;
  padding: 3px 8px;
  overflow: hidden;
  color: #fff;
  font-size: 11px;
  line-height: 1.35;
  white-space: nowrap;
  border-radius: 999px;
}
.created {
  background: var(--tm-accent-indigo);
}
.completed {
  background: var(--tm-accent-green);
}
.empty-chart {
  color: var(--tm-text-muted);
}
</style>
