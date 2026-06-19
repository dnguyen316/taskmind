<script setup lang="ts">
import { computed } from 'vue'
import type { ReportsAssigneeWorkload } from '../types'

const props = defineProps<{ rows: ReportsAssigneeWorkload[] }>()

const maxOpenTasks = computed(() => Math.max(1, ...props.rows.map((row) => row.openTasks)))
</script>

<template>
  <div class="workload-chart" aria-label="Open task workload by assignee">
    <div v-if="rows.length === 0" class="empty-chart">No assignee workload yet.</div>
    <div v-for="row in rows" v-else :key="row.userId" class="workload-row">
      <div class="workload-label">
        <strong>{{ row.userId }}</strong>
        <span>{{ row.openTasks }} open</span>
      </div>
      <div class="track">
        <span
          class="fill"
          :style="{ width: `${Math.max(6, (row.openTasks / maxOpenTasks) * 100)}%` }"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.workload-chart {
  display: grid;
  gap: 14px;
}
.workload-row {
  display: grid;
  gap: 6px;
}
.workload-label {
  display: flex;
  gap: 12px;
  justify-content: space-between;
  color: var(--tm-text);
  font-size: 12px;
}
.workload-label strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.workload-label span,
.empty-chart {
  color: var(--tm-text-muted);
}
.track {
  height: 9px;
  overflow: hidden;
  background: var(--tm-surface-subtle);
  border-radius: 999px;
}
.fill {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--tm-accent-orange), var(--tm-accent-indigo));
  border-radius: inherit;
}
</style>
