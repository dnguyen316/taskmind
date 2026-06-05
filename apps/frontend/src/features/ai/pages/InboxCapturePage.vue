<script setup lang="ts">
import { BulbOutlined, InboxOutlined } from '@ant-design/icons-vue'
import AppLayout from '../../tasks/components/AppLayout.vue'
</script>

<template>
  <AppLayout>
    <section class="placeholder-page">
      <a-card class="hero-card">
        <div class="icon"><InboxOutlined /></div>
        <a-tag color="blue">Coming in M08</a-tag>
        <h1>Inbox Capture</h1>
        <p>
          M08 adds the build-kit AI capture flow: drop an unstructured idea here, send it through
          the Core AI facade to Nova, and turn the result into typed tasks or project plans with
          deterministic fallbacks.
        </p>
      </a-card>

      <a-card title="Planned M08 capabilities">
        <ul class="planned-list">
          <li><BulbOutlined /> Natural-language capture into structured task candidates.</li>
          <li>
            <BulbOutlined /> Goal breakdown, weekly review, task describe/autocomplete, and
            translate helpers.
          </li>
          <li>
            <BulbOutlined /> FE → Core facade → Nova capability wiring with local fallback states.
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
  border: 1px solid #bfdbfe;
  border-radius: 16px;
}
.icon {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: #eff6ff;
  color: #2563eb;
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
</style>
