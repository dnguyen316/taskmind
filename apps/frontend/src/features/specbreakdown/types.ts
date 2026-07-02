export type SpecOutputType = 'MVP_PLAN' | 'SPRINT_PLAN' | 'ROADMAP' | 'BUG_TRIAGE' | 'PERSONAL_GOAL'
export type SpecNodeLevel = 'EPIC' | 'STORY' | 'TASK' | 'SUBTASK'
export type SpecDraftStatus =
  | 'DRAFT'
  | 'PROCESSING'
  | 'READY_FOR_REVIEW'
  | 'MATERIALIZED'
  | 'REJECTED'
  | 'FAILED'

export interface SpecTreeNode {
  id: string
  level: SpecNodeLevel
  title: string
  description?: string | null
  storyPoints?: number | null
  children?: SpecTreeNode[]
}

export interface SpecCandidateTree {
  outputType?: SpecOutputType
  nodes: SpecTreeNode[]
}

export interface SpecBreakdownDraft {
  id: string
  projectId: string
  ownerUserId: string
  title: string
  rawSpec: string
  candidateTree: string
  status: SpecDraftStatus
}

export interface CreateSpecDraftPayload {
  projectId: string
  title: string
  rawSpec: string
  candidateTree?: string
}

export interface MaterializeSpecDraftResponse {
  taskIds: string[]
}
