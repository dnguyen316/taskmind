export type ScheduledBlockStatus = 'SCHEDULED' | 'COMPLETED' | 'MISSED' | 'CANCELLED'

export interface SchedulingPreferences {
  id: string
  version: number
  userId: string
  workdayStart: string
  workdayEnd: string
  blockGranularityMinutes: number
  maxDailyFocusMinutes: number
  createdAt: string
  updatedAt: string
}

export interface UpdateSchedulingPreferencesPayload {
  version?: number | null
  workdayStart: string
  workdayEnd: string
  blockGranularityMinutes: number
  maxDailyFocusMinutes: number
}

export interface GenerateSchedulePayload {
  from?: string | null
  to?: string | null
}

export interface ScheduledBlock {
  id: string
  version: number
  userId: string
  taskId: string
  startsAt: string
  endsAt: string
  status: ScheduledBlockStatus
  rationale: string | null
  completedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface UpdateScheduledBlockPayload {
  version?: number | null
  startsAt?: string | null
  endsAt?: string | null
  rationale?: string | null
}

export interface RescheduleProposal {
  blockId: string
  taskId: string
  reason: string
}

export interface GenerateScheduleResponse {
  blocks: ScheduledBlock[]
  proposals: RescheduleProposal[]
}
