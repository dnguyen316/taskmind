<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Button, Card, Empty, List, Space, Tag, Typography, message } from 'ant-design-vue'
import {
  approveAiActionProposal,
  listAiActionProposals,
  rejectAiActionProposal,
  type AiActionProposal,
} from '../composables/useActionProposals'

const props = defineProps<{ jobId: string }>()
const proposals = ref<AiActionProposal[]>([])
const loading = ref(false)
const deciding = ref<string | null>(null)
const highImpact = computed(() =>
  proposals.value.filter((proposal) => proposal.status === 'PENDING'),
)

async function refresh() {
  loading.value = true
  try {
    proposals.value = await listAiActionProposals(props.jobId)
  } finally {
    loading.value = false
  }
}

async function decide(proposal: AiActionProposal, decision: 'approve' | 'reject') {
  deciding.value = proposal.id
  try {
    const updated =
      decision === 'approve'
        ? await approveAiActionProposal(props.jobId, proposal.id)
        : await rejectAiActionProposal(props.jobId, proposal.id)
    proposals.value = proposals.value.map((item) => (item.id === updated.id ? updated : item))
    message.success(decision === 'approve' ? 'Action approved' : 'Action rejected')
  } finally {
    deciding.value = null
  }
}

onMounted(refresh)
</script>

<template>
  <Card title="Review AI action proposals" :loading="loading">
    <Typography.Paragraph>
      High-impact AI actions are held for review before TaskMind executes comments, branch changes,
      pull requests, or task mutations.
    </Typography.Paragraph>
    <Empty v-if="!highImpact.length" description="No pending approval gates" />
    <List v-else :data-source="highImpact" item-layout="vertical">
      <template #renderItem="{ item }">
        <List.Item>
          <Space direction="vertical" style="width: 100%">
            <Space>
              <Tag color="red">{{ item.riskLevel }}</Tag>
              <Typography.Text strong>{{ item.proposedActionType }}</Typography.Text>
            </Space>
            <Typography.Text>{{ item.rationale }}</Typography.Text>
            <pre>{{ item.payloadPreview }}</pre>
            <Space>
              <Button
                type="primary"
                :loading="deciding === item.id"
                @click="decide(item, 'approve')"
              >
                Approve
              </Button>
              <Button danger :loading="deciding === item.id" @click="decide(item, 'reject')">
                Reject
              </Button>
            </Space>
          </Space>
        </List.Item>
      </template>
    </List>
  </Card>
</template>
