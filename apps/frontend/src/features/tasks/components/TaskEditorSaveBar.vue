<script setup lang="ts">
import { CheckOutlined } from '@ant-design/icons-vue'
defineProps<{ status: string; saving: boolean; canSave: boolean; hasChanges: boolean }>()
defineEmits<{ save: [] }>()
</script>
<template>
  <div class="document-save">
    <span class="save-status" role="status" aria-live="polite"
      ><CheckOutlined v-if="!hasChanges && !saving" aria-hidden="true" />{{ status }}</span
    >
    <a-button
      type="primary"
      :loading="saving"
      :disabled="!canSave"
      title="Save changes (Ctrl/Cmd+S)"
      @click="$emit('save')"
      >Save changes</a-button
    >
  </div>
</template>
<style scoped>
.document-save {
  display: flex;
  align-items: center;
  gap: 16px;
}
.save-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--tm-text-muted);
  font-size: 12px;
}
:deep(.ant-btn-primary) {
  background: #2563eb;
  border-color: #2563eb;
  color: white;
}
:deep(.ant-btn-primary:not(:disabled):hover) {
  background: #1d4ed8;
  border-color: #1d4ed8;
}
:deep(.ant-btn-primary:disabled) {
  background: var(--tm-surface-muted);
  color: var(--tm-text-muted);
  border-color: var(--tm-border);
}
@media (max-width: 760px) {
  .document-save {
    gap: 10px;
  }
  .save-status {
    font-size: 11px;
  }
}
@media (max-width: 420px) {
  .document-save {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
