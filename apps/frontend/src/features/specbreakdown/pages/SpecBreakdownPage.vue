<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppLayout from '../../tasks/components/AppLayout.vue'
import { listProjects } from '../../projects/api/projectsApi'
import type { Project } from '../../projects/types'
import {
  createSpecBreakdownDraft,
  materializeSpecBreakdownDraft,
  reviewSpecBreakdownDraft,
} from '../api/specBreakdownApi'
import type { SpecCandidateTree, SpecNodeLevel, SpecOutputType, SpecTreeNode } from '../types'

const outputOptions: { label: string; value: SpecOutputType }[] = [
  { label: 'MVP plan', value: 'MVP_PLAN' },
  { label: 'Sprint plan', value: 'SPRINT_PLAN' },
  { label: 'Roadmap', value: 'ROADMAP' },
  { label: 'Bug triage', value: 'BUG_TRIAGE' },
  { label: 'Personal goal', value: 'PERSONAL_GOAL' },
]

const projects = ref<Project[]>([])
const projectId = ref('')
const specText = ref('')
const outputType = ref<SpecOutputType>('MVP_PLAN')
const currentStep = ref(0)
const loading = ref(false)
const errorMessage = ref('')
const draftId = ref('')
const generatedTaskIds = ref<string[]>([])
const tree = ref<SpecCandidateTree>({ outputType: outputType.value, nodes: [] })

const canCreateDraft = computed(() => projectId.value && specText.value.trim().length >= 10)
const treeNodeCount = computed(() => countNodes(tree.value.nodes))

onMounted(async () => {
  projects.value = await listProjects().catch(() => [])
  projectId.value = projects.value[0]?.id ?? ''
})

async function generateDraft() {
  if (!canCreateDraft.value) {
    errorMessage.value = 'Choose a project and paste at least 10 characters of spec text.'
    return
  }

  loading.value = true
  errorMessage.value = ''
  tree.value = buildLocalPreviewTree(specText.value, outputType.value)

  try {
    const draft = await createSpecBreakdownDraft({
      projectId: projectId.value,
      title: titleForSpec(specText.value),
      rawSpec: specText.value,
      candidateTree: JSON.stringify(tree.value),
    })
    draftId.value = draft.id
    tree.value = parseCandidateTree(draft.candidateTree)
    currentStep.value = 2
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not create draft.'
  } finally {
    loading.value = false
  }
}

async function acceptDraft() {
  await saveReview(true, 4)
}

async function rejectDraft() {
  await saveReview(false, 0)
}

async function saveReview(accepted: boolean, nextStep = currentStep.value) {
  if (!draftId.value) {
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    await reviewSpecBreakdownDraft(draftId.value, accepted, tree.value)
    currentStep.value = nextStep
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not save review.'
  } finally {
    loading.value = false
  }
}

async function createTasks() {
  if (!draftId.value) {
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    await saveReview(true)
    const response = await materializeSpecBreakdownDraft(draftId.value)
    generatedTaskIds.value = response.taskIds
    currentStep.value = 5
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : 'Could not create tasks.'
  } finally {
    loading.value = false
  }
}

function updateNodeTitle(node: SpecTreeNode, value: string) {
  node.title = value
}

function addChild(parent: SpecTreeNode) {
  parent.children = parent.children ?? []
  parent.children.push({
    id: crypto.randomUUID(),
    level: nextLevel(parent.level),
    title: 'New generated work item',
    description: '',
    children: [],
  })
}

function removeNode(nodes: SpecTreeNode[], node: SpecTreeNode) {
  const index = nodes.findIndex((candidate) => candidate.id === node.id)
  if (index >= 0) {
    nodes.splice(index, 1)
    return
  }

  for (const candidate of nodes) {
    removeNode(candidate.children ?? [], node)
  }
}

function moveNode(nodes: SpecTreeNode[], index: number, direction: -1 | 1) {
  const target = index + direction
  if (target < 0 || target >= nodes.length) {
    return
  }
  const [node] = nodes.splice(index, 1)
  nodes.splice(target, 0, node)
}

function mergeWithPrevious(nodes: SpecTreeNode[], index: number) {
  if (index <= 0) {
    return
  }
  const [node] = nodes.splice(index, 1)
  const previous = nodes[index - 1]
  previous.title = `${previous.title} / ${node.title}`
  previous.description = [previous.description, node.description].filter(Boolean).join('\n\n')
  previous.children = [...(previous.children ?? []), ...(node.children ?? [])]
}

function buildLocalPreviewTree(
  rawSpec: string,
  selectedOutputType: SpecOutputType,
): SpecCandidateTree {
  const lines = rawSpec
    .split('\n')
    .map((line) => line.trim().replace(/^[-*#\d.)\s]+/, ''))
    .filter(Boolean)
    .slice(0, 4)
  const seedLines = lines.length ? lines : [titleForSpec(rawSpec)]

  return {
    outputType: selectedOutputType,
    nodes: seedLines.map((line, epicIndex) => ({
      id: crypto.randomUUID(),
      level: 'EPIC',
      title: line,
      description: `Generated ${labelForOutput(selectedOutputType)} epic from the pasted spec.`,
      children: [
        {
          id: crypto.randomUUID(),
          level: 'STORY',
          title: `Deliver ${line}`,
          description: 'Review, implement, and validate this slice.',
          storyPoints: epicIndex + 3,
          children: [
            {
              id: crypto.randomUUID(),
              level: 'SUBTASK',
              title: `Validate ${line}`,
              description: 'Confirm acceptance criteria and ownership before scheduling.',
              children: [],
            },
          ],
        },
      ],
    })),
  }
}

function parseCandidateTree(candidateTree: string): SpecCandidateTree {
  try {
    const parsed = JSON.parse(candidateTree) as SpecCandidateTree
    return {
      outputType: parsed.outputType ?? outputType.value,
      nodes: normalizeNodes(parsed.nodes ?? []),
    }
  } catch {
    return { outputType: outputType.value, nodes: [] }
  }
}

function normalizeNodes(nodes: SpecTreeNode[]): SpecTreeNode[] {
  return nodes.map((node) => ({
    ...node,
    id: node.id || crypto.randomUUID(),
    children: normalizeNodes(node.children ?? []),
  }))
}

function countNodes(nodes: SpecTreeNode[]): number {
  return nodes.reduce((total, node) => total + 1 + countNodes(node.children ?? []), 0)
}

function nextLevel(level: SpecNodeLevel): SpecNodeLevel {
  if (level === 'EPIC') return 'STORY'
  if (level === 'STORY') return 'TASK'
  return 'SUBTASK'
}

function labelForOutput(value: SpecOutputType) {
  return outputOptions.find((option) => option.value === value)?.label ?? 'MVP plan'
}

function titleForSpec(rawSpec: string) {
  return rawSpec.trim().split('\n')[0]?.slice(0, 80) || 'Spec breakdown draft'
}
</script>

<template>
  <AppLayout :task-count="treeNodeCount">
    <template #title>Spec breakdown</template>
    <template #subtitle
      >Turn a pasted spec or goal into an editable Epic → Story → Task tree.</template
    >
    <template #headerActions>
      <RouterLink to="/dashboard"><a-button>Back to dashboard</a-button></RouterLink>
    </template>

    <a-alert
      v-if="errorMessage"
      class="flow-alert"
      type="error"
      show-icon
      :message="errorMessage"
    />

    <a-steps :current="currentStep" class="flow-steps">
      <a-step title="Paste spec" />
      <a-step title="Output" />
      <a-step title="Generate" />
      <a-step title="Review" />
      <a-step title="Create" />
      <a-step title="Schedule" />
    </a-steps>

    <a-card v-if="currentStep === 0" title="Step 1: paste a spec or goal">
      <a-select v-model:value="projectId" class="project-select" placeholder="Choose project">
        <a-select-option v-for="project in projects" :key="project.id" :value="project.id">
          {{ project.name }} ({{ project.key }})
        </a-select-option>
      </a-select>
      <a-textarea
        v-model:value="specText"
        aria-label="Spec or goal"
        :rows="10"
        placeholder="Paste a PRD, customer problem, sprint goal, bug list, or personal goal..."
      />
      <a-button type="primary" :disabled="!canCreateDraft" @click="currentStep = 1">Next</a-button>
    </a-card>

    <a-card v-else-if="currentStep === 1" title="Step 2: choose output type">
      <a-radio-group v-model:value="outputType" class="output-options">
        <a-radio-button v-for="option in outputOptions" :key="option.value" :value="option.value">
          {{ option.label }}
        </a-radio-button>
      </a-radio-group>
      <div class="card-actions">
        <a-button @click="currentStep = 0">Back</a-button>
        <a-button type="primary" :loading="loading" @click="generateDraft">Generate tree</a-button>
      </div>
    </a-card>

    <a-card v-else title="Generated Epic → Story → Task → Subtask tree">
      <p class="muted">
        Reviewing {{ treeNodeCount }} node(s) for a {{ labelForOutput(outputType).toLowerCase() }}.
        Task creation uses Core /v1/spec-breakdown only.
      </p>

      <div class="tree-editor">
        <template v-for="(node, index) in tree.nodes" :key="node.id">
          <div class="tree-node level-0">
            <a-tag color="blue">{{ node.level }}</a-tag>
            <a-input :value="node.title" @update:value="updateNodeTitle(node, $event)" />
            <a-button size="small" @click="moveNode(tree.nodes, index, -1)">↑</a-button>
            <a-button size="small" @click="moveNode(tree.nodes, index, 1)">↓</a-button>
            <a-button size="small" @click="mergeWithPrevious(tree.nodes, index)">Merge</a-button>
            <a-button size="small" @click="addChild(node)">Add child</a-button>
            <a-button size="small" danger @click="removeNode(tree.nodes, node)">Reject</a-button>
          </div>
          <div
            v-for="(story, storyIndex) in node.children"
            :key="story.id"
            class="tree-node level-1"
          >
            <a-tag color="green">{{ story.level }}</a-tag>
            <a-input :value="story.title" @update:value="updateNodeTitle(story, $event)" />
            <a-button size="small" @click="moveNode(node.children ?? [], storyIndex, -1)"
              >↑</a-button
            >
            <a-button size="small" @click="moveNode(node.children ?? [], storyIndex, 1)"
              >↓</a-button
            >
            <a-button size="small" @click="mergeWithPrevious(node.children ?? [], storyIndex)"
              >Merge</a-button
            >
            <a-button size="small" @click="addChild(story)">Add child</a-button>
            <a-button size="small" danger @click="removeNode(tree.nodes, story)">Reject</a-button>
          </div>
          <div
            v-for="task in node.children?.flatMap((story) => story.children ?? [])"
            :key="task.id"
            class="tree-node level-2"
          >
            <a-tag color="purple">{{ task.level }}</a-tag>
            <a-input :value="task.title" @update:value="updateNodeTitle(task, $event)" />
            <a-button size="small" @click="addChild(task)">Add subtask</a-button>
            <a-button size="small" danger @click="removeNode(tree.nodes, task)">Reject</a-button>
          </div>
        </template>
      </div>

      <div class="card-actions">
        <a-button :disabled="loading" @click="rejectDraft">Reject draft</a-button>
        <a-button :loading="loading" @click="saveReview(true)">Save edits</a-button>
        <a-button v-if="currentStep < 4" type="primary" :loading="loading" @click="acceptDraft">
          Accept hierarchy
        </a-button>
        <a-button v-else type="primary" :loading="loading" @click="createTasks">
          Create project tasks in Core
        </a-button>
      </div>
    </a-card>

    <a-result
      v-if="currentStep === 5"
      status="success"
      title="Project tasks created"
      :sub-title="`${generatedTaskIds.length} root task(s) were created from the accepted hierarchy.`"
    >
      <template #extra>
        <RouterLink to="/calendar"
          ><a-button type="primary">Schedule this work</a-button></RouterLink
        >
        <RouterLink to="/tasks"><a-button>View created tasks</a-button></RouterLink>
      </template>
    </a-result>
  </AppLayout>
</template>

<style scoped>
.flow-alert,
.flow-steps,
.project-select,
.output-options {
  margin-bottom: 1rem;
}
.card-actions {
  display: flex;
  gap: 0.75rem;
  margin-top: 1rem;
}
.muted {
  color: var(--tm-text-muted);
}
.tree-editor {
  display: grid;
  gap: 0.75rem;
}
.tree-node {
  display: grid;
  grid-template-columns: auto minmax(12rem, 1fr) repeat(5, auto);
  gap: 0.5rem;
  align-items: center;
}
.level-1 {
  margin-left: 2rem;
}
.level-2 {
  margin-left: 4rem;
  grid-template-columns: auto minmax(12rem, 1fr) repeat(2, auto);
}
</style>
