import { apiClient } from '../../../lib/apiClient'
import type { ActivitySearchDocument } from '../types'

export interface SearchActivityOptions {
  query?: string
  size?: number
}

export async function searchActivity({ query = '', size = 20 }: SearchActivityOptions = {}) {
  const response = await apiClient.get<unknown>('/v1/activity/search', {
    params: {
      q: query.trim() || undefined,
      size,
    },
  })

  return adaptActivitySearchDocumentListResponse(response.data)
}

function adaptActivitySearchDocumentListResponse(data: unknown): ActivitySearchDocument[] {
  if (!Array.isArray(data)) {
    throw new Error('Invalid activity search response.')
  }

  return data.map(adaptActivitySearchDocumentResponse)
}

function adaptActivitySearchDocumentResponse(data: unknown): ActivitySearchDocument {
  if (!isObject(data)) {
    throw new Error('Invalid activity search document response.')
  }

  return {
    eventId: readRequiredString(data, 'eventId', 'activity search document'),
    eventType: readRequiredString(data, 'eventType', 'activity search document'),
    actorUserId: readRequiredString(data, 'actorUserId', 'activity search document'),
    userId: readRequiredString(data, 'userId', 'activity search document'),
    projectId: readNullableString(data, 'projectId'),
    entityType: readRequiredString(data, 'entityType', 'activity search document'),
    entityId: readRequiredString(data, 'entityId', 'activity search document'),
    title: readNullableString(data, 'title'),
    status: readNullableString(data, 'status'),
    payload: readPayload(data.payload),
    occurredAt: readRequiredString(data, 'occurredAt', 'activity search document'),
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

function readPayload(value: unknown): Record<string, unknown> | null {
  if (value === null || value === undefined) {
    return null
  }

  if (!isObject(value)) {
    throw new Error('Invalid activity search document response: payload must be an object or null.')
  }

  return value
}
