<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  completeOnboarding,
  listOnboardingTemplates,
  resetDemoWorkspace,
} from '../api/onboardingApi'
import type {
  OnboardingTemplate,
  PlanningStyle,
  StartMode,
  WorkspaceType,
} from '../api/onboardingApi'
import { useAuthStore } from '../../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const currentStep = ref(0)
const workspaceType = ref<WorkspaceType>('TEAM')
const planningStyle = ref<PlanningStyle>('SPRINT')
const startMode = ref<StartMode>('TEMPLATE')
const templateKey = ref('product-launch')
const templates = ref<OnboardingTemplate[]>([])
const isSubmitting = ref(false)

const canResetDemo = computed(() => import.meta.env.DEV || import.meta.env.MODE === 'demo')

onMounted(async () => {
  templates.value = await listOnboardingTemplates()
  templateKey.value = templates.value[0]?.key ?? 'product-launch'
})

async function finish() {
  isSubmitting.value = true
  try {
    const result = await completeOnboarding({
      workspaceType: workspaceType.value,
      planningStyle: planningStyle.value,
      startMode: startMode.value,
      templateKey: startMode.value === 'TEMPLATE' ? templateKey.value : undefined,
    })
    await auth.fetchCurrentUser()
    message.success('Workspace ready. Welcome to TaskMind!')
    await router.replace(result.projectId ? `/projects/${result.projectId}` : '/dashboard')
  } finally {
    isSubmitting.value = false
  }
}

async function resetDemo() {
  isSubmitting.value = true
  try {
    const result = await resetDemoWorkspace()
    await auth.fetchCurrentUser()
    message.success('Demo workspace reset.')
    await router.replace(`/projects/${result.projectId}`)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <main class="onboarding-page">
    <a-card class="onboarding-card" title="Set up your TaskMind workspace">
      <a-steps
        :current="currentStep"
        :items="[{ title: 'Workspace' }, { title: 'Planning' }, { title: 'Start' }]"
      />

      <section v-if="currentStep === 0" class="onboarding-step">
        <h2>Choose workspace type</h2>
        <a-radio-group v-model:value="workspaceType" button-style="solid" size="large">
          <a-radio-button value="TEAM">Team</a-radio-button>
          <a-radio-button value="SOLO">Solo</a-radio-button>
          <a-radio-button value="CLIENT">Client work</a-radio-button>
        </a-radio-group>
      </section>

      <section v-else-if="currentStep === 1" class="onboarding-step">
        <h2>Choose planning style</h2>
        <a-radio-group v-model:value="planningStyle" button-style="solid" size="large">
          <a-radio-button value="SPRINT">Sprint</a-radio-button>
          <a-radio-button value="KANBAN">Kanban</a-radio-button>
          <a-radio-button value="TIME_BLOCKING">Time blocking</a-radio-button>
        </a-radio-group>
      </section>

      <section v-else class="onboarding-step">
        <h2>Create your first workspace</h2>
        <a-radio-group v-model:value="startMode" class="start-grid">
          <a-radio value="BLANK">Create from blank</a-radio>
          <a-radio value="TEMPLATE">Use a template</a-radio>
          <a-radio value="SPEC">Create from spec</a-radio>
          <a-radio value="GITHUB">Import GitHub</a-radio>
          <a-radio value="JIRA">Import Jira</a-radio>
          <a-radio value="DEMO">Load demo data</a-radio>
        </a-radio-group>
        <a-select
          v-if="startMode === 'TEMPLATE'"
          v-model:value="templateKey"
          class="template-select"
        >
          <a-select-option v-for="template in templates" :key="template.key" :value="template.key">
            {{ template.name }} — {{ template.description }}
          </a-select-option>
        </a-select>
        <a-alert
          v-if="['SPEC', 'GITHUB', 'JIRA'].includes(startMode)"
          type="info"
          show-icon
          message="This setup creates a starter workspace now; connect the selected import source from the project later."
        />
      </section>

      <template #actions>
        <a-button :disabled="currentStep === 0 || isSubmitting" @click="currentStep -= 1"
          >Back</a-button
        >
        <a-button v-if="currentStep < 2" type="primary" @click="currentStep += 1"
          >Continue</a-button
        >
        <a-button v-else type="primary" :loading="isSubmitting" @click="finish"
          >Create workspace</a-button
        >
        <a-button v-if="canResetDemo" danger :loading="isSubmitting" @click="resetDemo"
          >Reset demo workspace</a-button
        >
      </template>
    </a-card>
  </main>
</template>

<style scoped>
.onboarding-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 32px;
  background: #f5f7fb;
}
.onboarding-card {
  width: min(880px, 100%);
}
.onboarding-step {
  display: grid;
  gap: 24px;
  min-height: 240px;
  align-content: center;
  padding: 32px 0;
}
.start-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}
.template-select {
  width: 100%;
}
</style>
