export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'ARCHIVED'
export type EnergyLevel = 'LOW' | 'MEDIUM' | 'HIGH'
export type TaskLevel = 'EPIC' | 'STORY' | 'TASK' | 'SUBTASK'
export type TaskType = string

export interface TaskTypeDefinition {
  id: string
  version: number | null
  projectId: string | null
  key: string
  name: string
  color: string | null
  icon: string | null
  system: boolean
  active: boolean
  sortOrder: number | null
  createdAt: string
  updatedAt: string
}

export interface Task {
  id: string
  version: number | null
  projectId: string
  userId: string
  taskKey: string | null
  assigneeId: string | null
  parentTaskId: string | null
  taskLevel: TaskLevel | null
  taskType: TaskType | null
  storyPoints: number | null
  releaseVersion: string | null
  deletedAt: string | null
  title: string
  description: string | null
  status: TaskStatus
  priority: number
  dueAt: string | null
  durationMinutes: number | null
  energyLevel: EnergyLevel | null
  source: string
  confidence: number | null
  createdAt: string
  updatedAt: string
}

export type MediaKind = 'IMAGE' | 'DOCUMENT' | 'AUDIO' | 'VIDEO' | 'OTHER'

export interface TaskAttachment {
  id: string
  version: number | null
  taskId: string
  ownerUserId: string
  fileName: string
  contentType: string
  sizeBytes: number
  mediaKind: MediaKind
  deletedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface ActivitySearchAssistResponse {
  query: string
  explanation: string | null
  suggestedFilters: string[]
}

export interface ActivitySearchSuggestion {
  label: string
  value: string
  entityType: string
  entityId: string
  eventType: string
  status: string | null
  title: string | null
  occurredAt: string
  routeName: string | null
  score: number
}

export interface ActivitySearchDocument {
  eventId: string
  eventType: string
  actorUserId: string
  userId: string
  projectId: string | null
  entityType: string
  entityId: string
  title: string | null
  status: string | null
  payload: Record<string, unknown> | null
  occurredAt: string
}

export type TaskSortBy = 'updatedAt' | 'dueAt' | 'priority'

export interface TaskFilters {
  status?: TaskStatus
  overdueOnly: boolean
  searchText: string
  projectId?: string
  sortBy?: TaskSortBy
}

export interface CreateTaskPayload {
  projectId: string
  userId: string
  source: string
  title: string
  description: string | null
  priority: number
  durationMinutes: number | null
  dueAt: string | null
  status: TaskStatus
  taskType?: string | null
}

export interface UpdateTaskPayload {
  version?: number | null
  projectId: string
  title: string
  description: string | null
  priority: number
  dueAt: string | null
  durationMinutes: number | null
  energyLevel: EnergyLevel | null
  status: TaskStatus
  taskType?: string | null
}

export interface CreateTaskFormValues {
  projectId: string
  title: string
  description: string
  priority: number
  durationMinutes: number | null | ''
  dueAt: string
  status: TaskStatus
  taskType: string | null
}
