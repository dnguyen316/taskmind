export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE' | 'ARCHIVED'
export type EnergyLevel = 'LOW' | 'MEDIUM' | 'HIGH'

export interface Task {
  id: string
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

export interface TaskFilters {
  status?: TaskStatus
  overdueOnly: boolean
}

export interface CreateTaskPayload {
  projectId: string
  userId: string
  source: string
  title: string
  description: string | null
  priority: number
  durationMinutes: number
  dueAt: string | null
  status: TaskStatus
}

export interface UpdateTaskPayload {
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
  durationMinutes: number
  dueAt: string
  status: TaskStatus
}

export interface Project {
  id: string
  name: string
  isActive?: boolean
}
