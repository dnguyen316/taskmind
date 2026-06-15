<script setup lang="ts">
import { BarChartOutlined, FilePdfOutlined } from '@ant-design/icons-vue'
import AppLayout from '../../tasks/components/AppLayout.vue'
</script>

<template>
  <AppLayout>
    <section class="placeholder-page">
      <a-card class="hero-card">
        <div class="icon"><BarChartOutlined /></div>
        <a-tag color="orange">Coming in M12</a-tag>
        <h1>Reports</h1>
        <p>
          M12 implements reports from Relay-populated analytics rollups, including
          week/month/quarter ranges, trends, status and priority segments, workload summaries, and
          PDF export.
        </p>
      </a-card>

      <a-card title="Planned M12 reporting scope">
        <ul class="planned-list">
          <li>
            <FilePdfOutlined /> <code>GET /v1/reports?range=week|month|quarter</code> KPIs and trend
            data.
          </li>
          <li>
            <FilePdfOutlined /> Charts for throughput, workload, status, and priority segments.
          </li>
          <li><FilePdfOutlined /> Frontend PDF export via jsPDF and jspdf-autotable.</li>
        </ul>
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
  border: 1px solid var(--tm-border);
  border-radius: 16px;
}
.icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: var(--tm-surface-subtle);
  color: var(--tm-accent-orange);
  font-size: 24px;
  margin-bottom: 12px;
}
h1 {
  margin: 10px 0 8px;
}
p {
  color: var(--tm-text-muted);
  max-width: 760px;
}
.planned-list {
  display: grid;
  gap: 12px;
  padding-left: 0;
  list-style: none;
  margin: 0;
  color: var(--tm-text);
}
.planned-list li {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
code {
  background: var(--tm-surface-subtle);
  border: 1px solid var(--tm-border-soft);
  border-radius: 6px;
  padding: 1px 5px;
}
</style>
