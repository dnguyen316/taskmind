<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Button, Card, Empty, List, Space, Tag, Typography, message } from 'ant-design-vue'
import {
  acceptAiProposal,
  listPendingAiProposals,
  previewAiProposal,
  rejectAiProposal,
  type AiProposal,
  type AiProposalImpactPreview,
} from '../composables/useProposalReview'

const proposals = ref<AiProposal[]>([])
const previews = ref<Record<string, AiProposalImpactPreview>>({})
const loading = ref(false)
const deciding = ref<string | null>(null)

async function refresh() {
  loading.value = true
  try {
    proposals.value = await listPendingAiProposals()
  } finally {
    loading.value = false
  }
}

async function loadPreview(proposal: AiProposal) {
  previews.value[proposal.id] = await previewAiProposal(proposal.id)
}

async function decide(proposal: AiProposal, decision: 'accept' | 'reject') {
  deciding.value = proposal.id
  try {
    const updated =
      decision === 'accept'
        ? await acceptAiProposal(proposal.id)
        : await rejectAiProposal(proposal.id)
    proposals.value = proposals.value.filter((item) => item.id !== updated.id)
    message.success(decision === 'accept' ? 'Proposal accepted' : 'Proposal rejected')
  } finally {
    deciding.value = null
  }
}

onMounted(refresh)
</script>

<template>
  <Card title="Review AI proposals" :loading="loading">
    <Typography.Paragraph>
      Capture, goal breakdown, spec breakdown, scheduler, and Nova chat suggestions remain proposals
      until Core records your approval decision.
    </Typography.Paragraph>
    <Empty v-if="!proposals.length" description="No pending AI proposals" />
    <List v-else :data-source="proposals" item-layout="vertical">
      <template #renderItem="{ item }">
        <List.Item>
          <Space direction="vertical" style="width: 100%">
            <Space wrap>
              <Tag color="blue">{{ item.actionType }}</Tag>
              <Tag>{{ item.source }}</Tag>
              <Typography.Text type="secondary">{{ item.model || item.provider }}</Typography.Text>
            </Space>
            <Typography.Text strong>{{ item.preview }}</Typography.Text>
            <Typography.Text v-if="item.rationale">{{ item.rationale }}</Typography.Text>
            <pre v-if="previews[item.id]">{{ previews[item.id] }}</pre>
            <Space>
              <Button @click="loadPreview(item)">Preview impact</Button>
              <Button
                type="primary"
                :loading="deciding === item.id"
                @click="decide(item, 'accept')"
              >
                Accept
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
