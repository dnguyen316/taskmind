<script setup lang="ts">
import { TeamOutlined, UsergroupAddOutlined } from '@ant-design/icons-vue'
import AppLayout from '../../tasks/components/AppLayout.vue'
</script>

<template>
  <AppLayout>
    <section class="placeholder-page">
      <a-card class="hero-card">
        <div class="icon"><TeamOutlined /></div>
        <a-tag color="green">Coming in M12</a-tag>
        <h1>Team</h1>
        <p>
          M12 adds the team directory and workload aggregation backed by analytics/read-model data.
          Until then, this page intentionally avoids mock team metrics so the UI does not mix
          placeholders with live task counts.
        </p>
      </a-card>

      <a-card title="Planned M12 read-model widgets">
        <ul class="planned-list">
          <li><UsergroupAddOutlined /> Team directory from <code>GET /v1/team/directory</code>.</li>
          <li>
            <UsergroupAddOutlined /> Workload by assignee/team from Relay-populated analytics
            rollups.
          </li>
          <li>
            <UsergroupAddOutlined /> Dashboard/team cache invalidation from relevant domain events.
          </li>
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
  border: 1px solid #bbf7d0;
  border-radius: 16px;
}
.icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: #f0fdf4;
  color: #16a34a;
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
.planned-list {
  display: grid;
  gap: 12px;
  padding-left: 0;
  list-style: none;
  margin: 0;
  color: #334155;
}
.planned-list li {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
code {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 1px 5px;
}
</style>
