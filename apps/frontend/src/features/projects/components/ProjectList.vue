<script setup lang="ts">
interface ProjectListItem {
  id: string
  name: string
  key: string
  description?: string | null
  ownerUserId?: string | null
}

withDefaults(
  defineProps<{
    projects: ProjectListItem[]
    loading?: boolean
    errorMessage?: string | null
  }>(),
  {
    loading: false,
    errorMessage: null,
  },
)
</script>

<template>
  <a-card title="Projects" class="surface-card">
    <a-alert
      v-if="errorMessage"
      type="error"
      show-icon
      :message="errorMessage"
      class="space-bottom"
    />

    <a-spin :spinning="loading">
      <a-empty v-if="!loading && projects.length === 0" description="No projects available." />

      <a-list v-else :data-source="projects" item-layout="vertical" class="project-list">
        <template #renderItem="{ item }">
          <a-list-item class="project-item">
            <a-list-item-meta>
              <template #title>
                <a-space>
                  <span class="project-name">{{ item.name }}</span>
                  <a-tag color="blue">{{ item.key }}</a-tag>
                </a-space>
              </template>
              <template #description>
                <a-space direction="vertical" size="small">
                  <span>{{ item.description || 'No description' }}</span>
                  <span><strong>Owner:</strong> {{ item.ownerUserId || '—' }}</span>
                </a-space>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-spin>
  </a-card>
</template>

<style scoped>
.surface-card {
  border-radius: 18px;
}
.space-bottom {
  margin-bottom: 12px;
}
.project-item {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  margin-bottom: 10px;
  padding: 12px 16px;
  background: #f8fafc;
}
.project-name {
  font-weight: 600;
}
</style>
