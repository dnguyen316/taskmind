import { apiClient } from '../../../lib/apiClient'
import type {
  GenerateSchedulePayload,
  GenerateScheduleResponse,
  RescheduleProposal,
  ScheduledBlock,
  ScheduledBlockStatus,
  SchedulingPreferences,
  UpdateScheduledBlockPayload,
  UpdateSchedulingPreferencesPayload,
} from '../types'

export async function getSchedulingPreferences() {
  const response = await apiClient.get<unknown>('/v1/scheduler/preferences')
  return adaptSchedulingPreferences(response.data)
}

export async function updateSchedulingPreferences(payload: UpdateSchedulingPreferencesPayload) {
  const response = await apiClient.put<unknown>('/v1/scheduler/preferences', payload)
  return adaptSchedulingPreferences(response.data)
}

export async function generateSchedule(payload?: GenerateSchedulePayload) {
  const response = await apiClient.post<unknown>('/v1/scheduler/generate', payload ?? {})
  return adaptGenerateScheduleResponse(response.data)
}

export async function listScheduledBlocks(params?: GenerateSchedulePayload) {
  const response = await apiClient.get<unknown>('/v1/scheduler/blocks', {
    params: {
      from: params?.from ?? undefined,
      to: params?.to ?? undefined,
    },
  })

  return adaptScheduledBlockList(response.data)
}

export async function updateScheduledBlock(blockId: string, payload: UpdateScheduledBlockPayload) {
  const response = await apiClient.patch<unknown>(`/v1/scheduler/blocks/${blockId}`, payload)
  return adaptScheduledBlock(response.data)
}

export async function completeScheduledBlock(blockId: string) {
  const response = await apiClient.post<unknown>(`/v1/scheduler/blocks/${blockId}/complete`)
  return adaptScheduledBlock(response.data)
}

function adaptGenerateScheduleResponse(data: unknown): GenerateScheduleResponse {
  if (!isObject(data)) {
    throw new Error('Invalid schedule generation response.')
  }

  return {
    blocks: adaptScheduledBlockList(data.blocks),
    proposals: adaptRescheduleProposalList(data.proposals),
  }
}

function adaptScheduledBlockList(data: unknown) {
  if (!Array.isArray(data)) {
    throw new Error('Invalid scheduled block list response.')
  }

  return data.map(adaptScheduledBlock)
}

function adaptRescheduleProposalList(data: unknown) {
  if (!Array.isArray(data)) {
    throw new Error('Invalid reschedule proposal list response.')
  }

  return data.map(adaptRescheduleProposal)
}

function adaptSchedulingPreferences(data: unknown): SchedulingPreferences {
  if (!isObject(data)) {
    throw new Error('Invalid scheduling preferences response.')
  }

  return {
    id: readRequiredString(data, 'id', 'scheduling preferences'),
    version: readRequiredNumber(data, 'version', 'scheduling preferences'),
    userId: readRequiredString(data, 'userId', 'scheduling preferences'),
    workdayStart: readRequiredString(data, 'workdayStart', 'scheduling preferences'),
    workdayEnd: readRequiredString(data, 'workdayEnd', 'scheduling preferences'),
    blockGranularityMinutes: readRequiredNumber(
      data,
      'blockGranularityMinutes',
      'scheduling preferences',
    ),
    maxDailyFocusMinutes: readRequiredNumber(
      data,
      'maxDailyFocusMinutes',
      'scheduling preferences',
    ),
    createdAt: readRequiredString(data, 'createdAt', 'scheduling preferences'),
    updatedAt: readRequiredString(data, 'updatedAt', 'scheduling preferences'),
  }
}

function adaptScheduledBlock(data: unknown): ScheduledBlock {
  if (!isObject(data)) {
    throw new Error('Invalid scheduled block response.')
  }

  const status = readRequiredString(data, 'status', 'scheduled block')

  if (!isScheduledBlockStatus(status)) {
    throw new Error('Invalid scheduled block response: unknown status.')
  }

  return {
    id: readRequiredString(data, 'id', 'scheduled block'),
    version: readRequiredNumber(data, 'version', 'scheduled block'),
    userId: readRequiredString(data, 'userId', 'scheduled block'),
    taskId: readRequiredString(data, 'taskId', 'scheduled block'),
    startsAt: readRequiredString(data, 'startsAt', 'scheduled block'),
    endsAt: readRequiredString(data, 'endsAt', 'scheduled block'),
    status,
    rationale: readNullableString(data, 'rationale'),
    completedAt: readNullableString(data, 'completedAt'),
    createdAt: readRequiredString(data, 'createdAt', 'scheduled block'),
    updatedAt: readRequiredString(data, 'updatedAt', 'scheduled block'),
  }
}

function adaptRescheduleProposal(data: unknown): RescheduleProposal {
  if (!isObject(data)) {
    throw new Error('Invalid reschedule proposal response.')
  }

  return {
    blockId: readRequiredString(data, 'blockId', 'reschedule proposal'),
    taskId: readRequiredString(data, 'taskId', 'reschedule proposal'),
    reason: readRequiredString(data, 'reason', 'reschedule proposal'),
  }
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function readRequiredString(data: Record<string, unknown>, key: string, resourceName: string) {
  const value = data[key]

  if (typeof value !== 'string' || value.length === 0) {
    throw new Error(`Invalid ${resourceName} response: missing ${key}.`)
  }

  return value
}

function readNullableString(data: Record<string, unknown>, key: string) {
  const value = data[key]

  if (value === null || value === undefined) {
    return null
  }

  if (typeof value !== 'string') {
    throw new Error(`Invalid response: ${key} must be a string or null.`)
  }

  return value
}

function readRequiredNumber(data: Record<string, unknown>, key: string, resourceName: string) {
  const value = data[key]

  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`Invalid ${resourceName} response: missing ${key}.`)
  }

  return value
}

function isScheduledBlockStatus(status: string): status is ScheduledBlockStatus {
  return ['SCHEDULED', 'COMPLETED', 'MISSED', 'CANCELLED'].includes(status)
}
