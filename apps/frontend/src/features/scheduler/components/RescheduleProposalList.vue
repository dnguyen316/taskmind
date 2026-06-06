<script setup lang="ts">
import { BulbOutlined } from '@ant-design/icons-vue'
import type { RescheduleProposal } from '../types'

const props = defineProps<{
  proposals: RescheduleProposal[]
}>()
</script>

<template>
  <a-card class="tm-card-surface" title="Reschedule proposals">
    <template #extra>
      <a-tag :color="props.proposals.length ? 'orange' : 'default'"
        >{{ props.proposals.length }} proposals</a-tag
      >
    </template>

    <a-empty
      v-if="!props.proposals.length"
      description="No missed-block proposals yet. Generate a schedule after work is missed to surface proposals."
    />

    <div v-else class="proposal-list">
      <article
        v-for="proposal in props.proposals"
        :key="`${proposal.blockId}-${proposal.taskId}`"
        class="proposal-card"
      >
        <div class="proposal-icon"><BulbOutlined /></div>
        <div>
          <strong>Move task {{ proposal.taskId.slice(0, 8) }}</strong>
          <p>{{ proposal.reason }}</p>
          <span>Missed block {{ proposal.blockId.slice(0, 8) }}</span>
        </div>
      </article>
    </div>
  </a-card>
</template>

<style scoped>
.proposal-list {
  display: grid;
  gap: 12px;
}

.proposal-card {
  align-items: flex-start;
  border: 1px solid #fed7aa;
  border-radius: 14px;
  display: flex;
  gap: 12px;
  padding: 14px;
}

.proposal-icon {
  background: #fff7ed;
  border-radius: 12px;
  color: #ea580c;
  display: grid;
  flex: 0 0 auto;
  height: 38px;
  place-items: center;
  width: 38px;
}

p {
  color: #475569;
  margin: 4px 0;
}

span {
  color: #64748b;
  font-size: 12px;
}
</style>
