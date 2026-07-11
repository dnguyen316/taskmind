<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { TeamOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import AppLayout from '../../tasks/components/AppLayout.vue'
import { useAuthStore } from '../../../stores/auth'
import { GLOBAL_ROLE_OPTIONS, PROJECT_ROLE_OPTIONS, useTeamStore } from '../../../stores/team'
import type { ProjectMembershipRole } from '../../projects/types'
import type { TeamMember } from '../types'

const authStore = useAuthStore()
const teamStore = useTeamStore()
const { directory, members, memberCount, totalOpenTasks, allocationsByUserId, activeProjects } =
  storeToRefs(teamStore)

const allocationModalOpen = ref(false)
const selectedMember = ref<TeamMember | null>(null)
const allocationForm = reactive<{ projectId: string; role: ProjectMembershipRole }>({
  projectId: '',
  role: 'MEMBER',
})

const canManageTeam = computed(() => authStore.canManageTeam)
const canManageGlobalRoles = computed(() => authStore.canManageGlobalRoles)
const hasMembers = computed(() => members.value.length > 0)
const tableLoading = computed(() => teamStore.loading.directory || teamStore.loading.allocations)

const memberRows = computed(() =>
  members.value.map((member) => ({
    key: member.userId,
    allocations: allocationsByUserId.value[member.userId] ?? [],
    ...member,
  })),
)

const allocationOptions = computed(() => {
  const existingProjectIds = new Set(
    selectedMember.value
      ? (allocationsByUserId.value[selectedMember.value.userId] ?? []).map(
          (allocation) => allocation.projectId,
        )
      : [],
  )

  return activeProjects.value.filter((project) => !existingProjectIds.has(project.id))
})

const columns = computed(() => [
  { title: 'Member', dataIndex: 'displayName', key: 'displayName' },
  { title: 'Email', dataIndex: 'email', key: 'email' },
  { title: 'Open tasks', dataIndex: 'openTasks', key: 'openTasks', align: 'right' as const },
  { title: 'Projects', dataIndex: 'allocations', key: 'allocations' },
  ...(canManageGlobalRoles.value
    ? [{ title: 'Global role', dataIndex: 'globalRole', key: 'globalRole' }]
    : []),
  ...(canManageTeam.value ? [{ title: 'Actions', key: 'actions' }] : []),
])

onMounted(() => {
  void refreshTeam()
})

async function refreshTeam() {
  try {
    await Promise.all([
      teamStore.fetchDirectory({ force: true }),
      teamStore.fetchAllocations({ force: true }),
    ])
  } catch {
    // The store exposes the normalized error message in-page.
  }
}

function openAllocationModal(member: TeamMember) {
  selectedMember.value = member
  allocationForm.projectId = allocationOptions.value[0]?.id ?? ''
  allocationForm.role = 'MEMBER'
  allocationModalOpen.value = true
}

async function submitAllocation() {
  if (!selectedMember.value || !allocationForm.projectId) {
    return
  }

  try {
    await teamStore.assignMemberToProject(
      selectedMember.value.userId,
      allocationForm.projectId,
      allocationForm.role,
    )
    message.success('Project allocation saved.')
    allocationModalOpen.value = false
  } catch {
    message.error(teamStore.messages.error || 'Failed to save project allocation.')
  }
}

async function changeProjectRole(userId: string, projectId: string, role: ProjectMembershipRole) {
  try {
    await teamStore.updateProjectRole(userId, projectId, role)
    message.success('Project role updated.')
  } catch {
    message.error(teamStore.messages.error || 'Failed to update project role.')
  }
}

async function removeAllocation(userId: string, projectId: string) {
  try {
    await teamStore.removeMemberFromProject(userId, projectId)
    message.success('Project allocation removed.')
  } catch {
    message.error(teamStore.messages.error || 'Failed to remove project allocation.')
  }
}

function onProjectRoleChange(userId: string, projectId: string, role: unknown) {
  void changeProjectRole(userId, projectId, role as ProjectMembershipRole)
}

function onGlobalRoleChange(userId: string, role: unknown) {
  void changeGlobalRole(userId, String(role))
}

async function changeGlobalRole(userId: string, role: string) {
  try {
    await teamStore.updateGlobalRole(userId, role)
    message.success('Global role updated.')
  } catch {
    message.error(teamStore.messages.error || 'Failed to update global role.')
  }
}
</script>

<template>
  <AppLayout>
    <section class="team-page">
      <a-card class="hero-card">
        <div class="hero-copy">
          <div class="icon"><TeamOutlined /></div>
          <a-tag color="green">Core RBAC</a-tag>
          <h1>Team management</h1>
          <p>
            Review the Core team directory, assign members to projects, and manage project or global
            roles according to your account permissions.
          </p>
        </div>
        <div class="summary-grid">
          <a-statistic title="Members" :value="memberCount" />
          <a-statistic title="Open tasks" :value="totalOpenTasks" />
        </div>
      </a-card>

      <a-alert
        v-if="teamStore.messages.error"
        type="error"
        show-icon
        :message="teamStore.messages.error"
        description="Refresh the directory or confirm your account has team/project membership permissions."
      />

      <a-card title="Members">
        <template #extra>
          <a-space>
            <a-tag v-if="canManageTeam" color="blue">Project membership manager</a-tag>
            <a-tag v-if="canManageGlobalRoles" color="purple">Global RBAC manager</a-tag>
            <a-button :loading="tableLoading" @click="refreshTeam">Refresh</a-button>
          </a-space>
        </template>

        <a-skeleton v-if="tableLoading && !directory" active :paragraph="{ rows: 4 }" />
        <a-empty
          v-else-if="!hasMembers"
          description="No team members are available from Core yet."
        />
        <a-table
          v-else
          :columns="columns"
          :data-source="memberRows"
          :loading="tableLoading"
          :pagination="false"
          row-key="userId"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'displayName'">
              <strong>{{ record.displayName }}</strong>
            </template>

            <template v-else-if="column.key === 'allocations'">
              <a-space
                v-if="record.allocations.length"
                direction="vertical"
                class="allocations-list"
              >
                <a-space
                  v-for="allocation in record.allocations"
                  :key="allocation.projectId"
                  wrap
                  class="allocation-row"
                >
                  <a-tag color="geekblue">{{ allocation.projectKey }}</a-tag>
                  <span>{{ allocation.projectName }}</span>
                  <a-select
                    v-if="canManageTeam"
                    :value="allocation.role"
                    size="small"
                    class="role-select"
                    :loading="
                      teamStore.savingProjectRoleByKey[
                        teamStore.projectRoleKey(record.userId, allocation.projectId)
                      ]
                    "
                    @change="
                      (role: unknown) =>
                        onProjectRoleChange(record.userId, allocation.projectId, role)
                    "
                  >
                    <a-select-option v-for="role in PROJECT_ROLE_OPTIONS" :key="role" :value="role">
                      {{ role }}
                    </a-select-option>
                  </a-select>
                  <a-tag v-else>{{ allocation.role }}</a-tag>
                  <a-button
                    v-if="canManageTeam"
                    type="link"
                    danger
                    size="small"
                    :loading="
                      teamStore.savingProjectRoleByKey[
                        teamStore.projectRoleKey(record.userId, allocation.projectId)
                      ]
                    "
                    @click="removeAllocation(record.userId, allocation.projectId)"
                  >
                    Remove
                  </a-button>
                </a-space>
              </a-space>
              <span v-else class="muted">No project allocations</span>
            </template>

            <template v-else-if="column.key === 'globalRole'">
              <a-select
                :value="record.globalRole ?? 'MEMBER'"
                class="role-select"
                :loading="teamStore.savingGlobalRoleByUserId[record.userId]"
                @change="(role: unknown) => onGlobalRoleChange(record.userId, role)"
              >
                <a-select-option v-for="role in GLOBAL_ROLE_OPTIONS" :key="role" :value="role">
                  {{ role }}
                </a-select-option>
              </a-select>
            </template>

            <template v-else-if="column.key === 'actions'">
              <a-button type="primary" size="small" @click="openAllocationModal(record)">
                Assign project
              </a-button>
            </template>
          </template>
        </a-table>
      </a-card>

      <a-modal
        v-model:open="allocationModalOpen"
        title="Assign project"
        ok-text="Assign"
        :ok-button-props="{ disabled: !allocationForm.projectId }"
        :confirm-loading="
          selectedMember && allocationForm.projectId
            ? teamStore.savingProjectRoleByKey[
                teamStore.projectRoleKey(selectedMember.userId, allocationForm.projectId)
              ]
            : false
        "
        @ok="submitAllocation"
      >
        <a-form layout="vertical">
          <a-form-item label="Member">
            <a-input :value="selectedMember?.displayName" disabled />
          </a-form-item>
          <a-form-item label="Project">
            <a-select v-model:value="allocationForm.projectId" placeholder="Choose a project">
              <a-select-option
                v-for="project in allocationOptions"
                :key="project.id"
                :value="project.id"
              >
                {{ project.key }} · {{ project.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="Project role">
            <a-select v-model:value="allocationForm.role">
              <a-select-option v-for="role in PROJECT_ROLE_OPTIONS" :key="role" :value="role">
                {{ role }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
        <a-empty
          v-if="allocationOptions.length === 0"
          description="This member is already allocated to every active project."
        />
      </a-modal>
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
.allocations-list,
.allocation-row {
  width: 100%;
}
.role-select {
  min-width: 120px;
}
.muted {
  color: var(--tm-text-muted);
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
