<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { listAiWorkflowTemplates } from '../../aiWorkflows/api/aiWorkflowTemplatesApi'
import { listProjectLinks } from '../../integrations/api/integrationsApi'
import {
  approveAiTaskResolutionProposal,
  createAiTaskResolutionJob,
  listAiTaskResolutionJobs,
  listAiTaskResolutionProposals,
  rejectAiTaskResolutionProposal,
} from '../api/aiResolutionApi'
import type { AiWorkflowTemplate } from '../../aiWorkflows/types'
import type { IntegrationProjectLink } from '../../integrations/types'
import type { AiTaskResolutionJob, AiTaskResolutionProposal } from '../types'
const props = defineProps<{ taskId: string; projectId: string }>()
const templates = ref<AiWorkflowTemplate[]>([])
const links = ref<IntegrationProjectLink[]>([])
const jobs = ref<AiTaskResolutionJob[]>([])
const proposals = ref<AiTaskResolutionProposal[]>([])
const templateId = ref<string>()
const linkId = ref<string>()
const loading = ref(false)
const running = ref(false)
const error = ref('')
const notice = ref('')
const latestJob = computed(() => jobs.value[0] ?? null)
const isEmpty = computed(() => !loading.value && !jobs.value.length)
async function load() {
  if (!props.taskId || !props.projectId) return
  loading.value = true
  error.value = ''
  try {
    const [templateList, linkList, jobList] = await Promise.all([
      listAiWorkflowTemplates(props.projectId),
      listProjectLinks(props.projectId),
      listAiTaskResolutionJobs(props.taskId),
    ])
    templates.value = templateList.filter((t) => t.workflowType === 'TASK_RESOLUTION')
    links.value = linkList.filter((l) => l.provider === 'GITHUB')
    jobs.value = [...jobList].sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    if (jobs.value[0]) proposals.value = await listAiTaskResolutionProposals(jobs.value[0].id)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Some AI agent data could not be loaded.'
  } finally {
    loading.value = false
  }
}
async function resolveWithAi() {
  running.value = true
  error.value = ''
  notice.value = ''
  try {
    const job = await createAiTaskResolutionJob(props.taskId, {
      templateId: templateId.value ?? null,
      githubProjectLinkId: linkId.value ?? null,
      idempotencyKey: crypto.randomUUID?.() ?? String(Date.now()),
    })
    jobs.value.unshift(job)
    proposals.value = []
    notice.value = 'AI resolution job queued.'
  } catch (e) {
    error.value =
      e instanceof Error ? e.message : 'AI resolution could not be queued. Please retry.'
  } finally {
    running.value = false
  }
}
async function decide(proposal: AiTaskResolutionProposal, decision: 'approve' | 'reject') {
  if (!latestJob.value) return
  const updated =
    decision === 'approve'
      ? await approveAiTaskResolutionProposal(latestJob.value.id, proposal.id)
      : await rejectAiTaskResolutionProposal(latestJob.value.id, proposal.id)
  proposals.value = proposals.value.map((item) => (item.id === updated.id ? updated : item))
}
function linksFromSummary(summary: string | null) {
  return (summary?.match(/https?:\/\/\S+/g) ?? []).map((url) => url.replace(/[),.]$/, ''))
}
onMounted(load)
watch(() => [props.taskId, props.projectId], load)
</script>
<template>
  <a-card title="Resolve with AI Agent" class="ai-resolution-card"
    ><a-space direction="vertical" style="width: 100%" size="middle"
      ><a-alert
        v-if="error"
        type="warning"
        show-icon
        message="Recoverable error"
        :description="error"
      /><a-alert v-if="notice" type="success" show-icon :message="notice" /><a-spin
        v-if="loading"
        tip="Loading AI agent context..."
      /><template v-else
        ><a-row :gutter="12"
          ><a-col :xs="24" :md="10"
            ><a-select
              v-model:value="templateId"
              allow-clear
              style="width: 100%"
              placeholder="Select workflow template"
              ><a-select-option
                v-for="template in templates"
                :key="template.id"
                :value="template.id"
                >{{ template.name }}</a-select-option
              ></a-select
            ></a-col
          ><a-col :xs="24" :md="10"
            ><a-select
              v-model:value="linkId"
              allow-clear
              style="width: 100%"
              placeholder="Select linked GitHub repo"
              ><a-select-option v-for="link in links" :key="link.id" :value="link.id"
                >{{ link.repositoryOwner }}/{{ link.repositoryName }}</a-select-option
              ></a-select
            ></a-col
          ><a-col :xs="24" :md="4"
            ><a-button type="primary" block :loading="running" @click="resolveWithAi"
              >Resolve with AI Agent</a-button
            ></a-col
          ></a-row
        ><a-empty
          v-if="isEmpty"
          description="No AI resolution jobs have been run for this task."
        /><a-timeline v-else
          ><a-timeline-item
            v-for="job in jobs"
            :key="job.id"
            :color="job.status === 'FAILED' ? 'red' : job.status === 'SUCCEEDED' ? 'green' : 'blue'"
            ><strong>{{ job.status }}</strong> — {{ job.currentStep || 'Awaiting next step'
            }}<br /><small>{{ job.createdAt }}</small></a-timeline-item
          ></a-timeline
        ><a-card v-if="proposals.length" size="small" title="Proposal approvals"
          ><a-list :data-source="proposals"
            ><template #renderItem="{ item }"
              ><a-list-item
                ><template #actions
                  ><a-button
                    size="small"
                    :disabled="item.status !== 'PENDING'"
                    @click="decide(item, 'approve')"
                    >Approve</a-button
                  ><a-button
                    danger
                    size="small"
                    :disabled="item.status !== 'PENDING'"
                    @click="decide(item, 'reject')"
                    >Reject</a-button
                  ></template
                ><a-list-item-meta
                  :title="`${item.proposedActionType} · ${item.riskLevel}`"
                  :description="item.rationale || item.payloadPreview"
                /><a-tag>{{ item.status }}</a-tag></a-list-item
              ></template
            ></a-list
          ></a-card
        ><a-card v-if="latestJob?.resultSummary" size="small" title="Final result summary"
          ><p>{{ latestJob.resultSummary }}</p>
          <a-space wrap
            ><a
              v-for="url in linksFromSummary(latestJob.resultSummary)"
              :key="url"
              :href="url"
              target="_blank"
              rel="noreferrer"
              >{{ url }}</a
            ></a-space
          ></a-card
        ></template
      ></a-space
    ></a-card
  >
</template>
<style scoped>
.ai-resolution-card {
  border-radius: 18px;
}
</style>
