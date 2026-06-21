import { apiClient } from '../../../lib/apiClient'
import type {
  ActivitySearchAssistResponse,
  ActivitySearchDocument,
  ActivitySearchSuggestion,
} from '../types'

export interface ActivitySearchFilters {
  entityType?: string
  status?: string
  projectId?: string
  from?: string
  to?: string
  eventType?: string
}

export interface SearchActivityOptions extends ActivitySearchFilters {
  query?: string
  size?: number
}

export interface SuggestActivitySearchOptions extends ActivitySearchFilters {
  query: string
  size?: number
}

export async function recommendActivitySearch({
  query,
  size = 10,
  ...filters
}: SuggestActivitySearchOptions) {
  const response = await apiClient.get<unknown>('/v1/activity/search/recommendations', {
    params: {
      q: query.trim(),
      size,
      ...cleanFilters(filters),
    },
  })

  return adaptActivitySearchSuggestionListResponse(response.data)
}

export async function suggestActivitySearch(options: SuggestActivitySearchOptions) {
  const recommendations = await recommendActivitySearch(options)

  return recommendations.map((recommendation) => recommendation.value)
}

export async function assistActivitySearch(prompt: string, currentQuery?: string) {
  const response = await apiClient.post<unknown>('/v1/activity/search/assist', {
    prompt: prompt.trim(),
    currentQuery: currentQuery?.trim() || null,
  })

  return adaptActivitySearchAssistResponse(response.data)
}

export async function searchActivity({
  query = '',
  size = 20,
  ...filters
}: SearchActivityOptions = {}) {
  const response = await apiClient.get<unknown>('/v1/activity/search', {
    params: {
      q: query.trim() || undefined,
      size,
      ...cleanFilters(filters),
    },
  })

  return adaptActivitySearchDocumentListResponse(response.data)
}

function cleanFilters(filters: ActivitySearchFilters) {
  return Object.fromEntries(
    Object.entries(filters)
      .map(([key, value]) => [key, typeof value === 'string' ? value.trim() : value])
      .filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
}

function adaptActivitySearchAssistResponse(data: unknown): ActivitySearchAssistResponse {
  if (!isObject(data)) {
    throw new Error('Invalid activity search assist response.')
  }

  const suggestedFilters = data.suggestedFilters
  if (
    !Array.isArray(suggestedFilters) ||
    suggestedFilters.some((value) => typeof value !== 'string')
  ) {
    throw new Error('Invalid activity search assist response: suggestedFilters must be strings.')
  }

  return {
    query: readRequiredString(data, 'query', 'activity search assist'),
    explanation: readNullableString(data, 'explanation'),
    suggestedFilters,
  }
}

function adaptActivitySearchDocumentListResponse(data: unknown): ActivitySearchDocument[] {
  if (!Array.isArray(data)) {
    throw new Error('Invalid activity search response.')
  }

  return data.map(adaptActivitySearchDocumentResponse)
}

function adaptActivitySearchSuggestionListResponse(data: unknown): ActivitySearchSuggestion[] {
  if (!Array.isArray(data)) {
    throw new Error('Invalid activity search recommendations response.')
  }

  return data.map(adaptActivitySearchSuggestionResponse)
}

function adaptActivitySearchSuggestionResponse(data: unknown): ActivitySearchSuggestion {
  if (typeof data === 'string') {
    return {
      label: data,
      value: data,
      entityType: 'activity',
      entityId: '',
      eventType: '',
      status: null,
      title: data,
      occurredAt: '',
      routeName: null,
    }
  }

  if (!isObject(data)) {
    throw new Error('Invalid activity search recommendation response.')
  }

  return {
    label: readRequiredString(data, 'label', 'activity search recommendation'),
    value: readRequiredString(data, 'value', 'activity search recommendation'),
    entityType: readRequiredString(data, 'entityType', 'activity search recommendation'),
    entityId: readRequiredString(data, 'entityId', 'activity search recommendation'),
    eventType: readRequiredString(data, 'eventType', 'activity search recommendation'),
    status: readNullableString(data, 'status'),
    title: readNullableString(data, 'title'),
    occurredAt: readRequiredString(data, 'occurredAt', 'activity search recommendation'),
    routeName: readNullableString(data, 'routeName'),
  }
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
