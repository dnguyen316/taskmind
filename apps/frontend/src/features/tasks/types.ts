export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'ARCHIVED'
export type EnergyLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export interface Task {
  id: string
  version: number | null
  projectId: string
  userId: string
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
}

export interface CreateTaskFormValues {
  projectId: string
  title: string
  description: string
  priority: number
  durationMinutes: number | null | ''
  dueAt: string
  status: TaskStatus
}
