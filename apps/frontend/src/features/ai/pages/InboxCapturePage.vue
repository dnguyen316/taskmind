<script setup lang="ts">
import { InboxOutlined } from '@ant-design/icons-vue'
import CapturePanel from '../components/CapturePanel.vue'
import GoalBreakdownPanel from '../components/GoalBreakdownPanel.vue'
import WeeklyReviewPanel from '../components/WeeklyReviewPanel.vue'
import AppLayout from '../../tasks/components/AppLayout.vue'
</script>

<template>
  <AppLayout>
    <section class="placeholder-page">
      <a-card class="hero-card">
        <div class="icon"><InboxOutlined /></div>
        <a-tag color="blue">AI workspace</a-tag>
        <h1>Turn ideas into action</h1>
        <p>
          Capture messy notes with AI, break goals into practical next steps, and generate a weekly
          review that helps you decide what to focus on next.
        </p>
      </a-card>

      <CapturePanel />
      <GoalBreakdownPanel />
      <WeeklyReviewPanel />

      <a-collapse class="upcoming-collapse" ghost>
        <a-collapse-panel key="upcoming-ai-features" header="Upcoming AI features">
          <p class="upcoming-intro">
            More planning, review, and assistant workflows will land here as they become ready. The
            primary workspace above only highlights AI tools that are currently rendered on this
            page.
          </p>
        </a-collapse-panel>
      </a-collapse>
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
.upcoming-collapse {
  border-radius: 12px;
  background: #f8fafc;
}
.upcoming-intro {
  margin: 0;
}
</style>
