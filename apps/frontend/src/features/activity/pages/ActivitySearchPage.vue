<script setup lang="ts">
import AppLayout from '../../tasks/components/AppLayout.vue'
import ActivitySearchPanel from '../components/ActivitySearchPanel.vue'
</script>

<template>
  <AppLayout>
    <section class="activity-search-page">
      <header class="topbar">
        <div>
          <h1>Activity</h1>
          <p>Search Core activity events indexed by Relay.</p>
        </div>
      </header>

      <ActivitySearchPanel />
    </section>
  </AppLayout>
</template>

<style scoped>
.activity-search-page {
  display: grid;
  gap: 16px;
}

.topbar h1 {
  margin: 0;
  font-size: 32px;
}

.topbar p {
  margin: 4px 0 0;
  color: #64748b;
}
</style>
