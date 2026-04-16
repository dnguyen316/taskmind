<script setup>
const props = defineProps({
  id: {
    type: String,
    required: true,
  },
})
</script>

<template>
  <main class="task-detail-page">
    <a-card title="Task detail" class="surface-card">
      <p class="task-id">Editing task: {{ props.id }}</p>
      <p class="hint">Task detail/edit view is now route-ready.</p>
    </a-card>
  </main>
</template>

<style scoped>
.task-detail-page {
  min-height: 100vh;
  max-width: 1100px;
  margin: 0 auto;
  padding: 32px 20px 40px;
}

.surface-card {
  border-radius: 18px;
}

.task-id {
  margin: 0;
  font-weight: 600;
}

.hint {
  margin: 6px 0 0;
  color: #64748b;
}
</style>
