<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { TeamOutlined } from '@ant-design/icons-vue'
import AppLayout from '../../tasks/components/AppLayout.vue'
import { useTeamDirectory } from '../composables/useTeamDirectory'

const { directory, members, hasMembers, loading, errorMessage, fetchDirectory } = useTeamDirectory()

const memberRows = computed(() =>
  members.value.map((member) => ({
    key: member.userId,
    ...member,
  })),
)

const columns = [
  { title: 'Member', dataIndex: 'displayName', key: 'displayName' },
  { title: 'Email', dataIndex: 'email', key: 'email' },
  { title: 'Open tasks', dataIndex: 'openTasks', key: 'openTasks', align: 'right' as const },
]

onMounted(() => {
  void fetchDirectory().catch(() => undefined)
})
</script>

<template>
  <AppLayout>
    <section class="team-page">
      <a-card class="hero-card">
        <div class="hero-copy">
          <div class="icon"><TeamOutlined /></div>
          <a-tag color="green">Live Core data</a-tag>
          <h1>Team directory</h1>
          <p>
            This page loads the privileged team directory from Core and shows real workload totals
            from analytics-backed read models.
          </p>
        </div>
        <div class="summary-grid">
          <a-statistic title="Members" :value="directory?.totalMembers ?? 0" />
          <a-statistic title="Open tasks" :value="directory?.totalOpenTasks ?? 0" />
        </div>
      </a-card>

      <a-alert
        v-if="errorMessage"
        type="error"
        show-icon
        :message="errorMessage"
        description="Refresh the directory or confirm your account has manager/admin access."
      />

      <a-card title="Members">
        <template #extra>
          <a-button :loading="loading" @click="fetchDirectory">Refresh</a-button>
        </template>

        <a-skeleton v-if="loading && !directory" active :paragraph="{ rows: 4 }" />
        <a-empty
          v-else-if="!hasMembers"
          description="No team members are available from Core yet."
        />
        <a-table
          v-else
          :columns="columns"
          :data-source="memberRows"
          :pagination="false"
          row-key="userId"
        />
      </a-card>
    </section>
  </AppLayout>
</template>

<style scoped>
.team-page {
  display: grid;
  gap: 16px;
}
.hero-card :deep(.ant-card-body) {
  display: flex;
  gap: 24px;
  align-items: center;
  justify-content: space-between;
}
.hero-card {
  border: 1px solid var(--tm-border);
  border-radius: 16px;
}
.hero-copy {
  min-width: 0;
}
.icon {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
  display: grid;
  color: var(--tm-accent-green);
  font-size: 24px;
  background: var(--tm-surface-subtle);
  border-radius: 16px;
  place-items: center;
}
h1 {
  margin: 10px 0 8px;
}
p {
  max-width: 760px;
  color: var(--tm-text-muted);
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(120px, 1fr));
  gap: 16px;
  min-width: 280px;
}
@media (max-width: 760px) {
  .hero-card :deep(.ant-card-body) {
    align-items: stretch;
    flex-direction: column;
  }
  .summary-grid {
    min-width: 0;
  }
}
</style>
