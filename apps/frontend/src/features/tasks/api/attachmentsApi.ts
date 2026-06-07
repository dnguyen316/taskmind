import { apiClient } from '../../../lib/apiClient'
import type { MediaKind, TaskAttachment } from '../types'

export interface UploadTaskAttachmentPayload {
  file: File
  mediaKind: MediaKind
}

export interface DownloadTaskAttachmentResult {
  blob: Blob
  fileName: string | null
  contentType: string | null
}

export async function uploadTaskAttachment(taskId: string, payload: UploadTaskAttachmentPayload) {
  const formData = new FormData()
  formData.append('file', payload.file)
  formData.append('mediaKind', payload.mediaKind)

  const response = await apiClient.post<unknown>(`/v1/tasks/${taskId}/attachments`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })

  return adaptTaskAttachmentResponse(response.data)
}

export async function listTaskAttachments(taskId: string) {
  const response = await apiClient.get<unknown>(`/v1/tasks/${taskId}/attachments`)
  return adaptTaskAttachmentListResponse(response.data)
}

export async function downloadTaskAttachment(taskId: string, attachmentId: string) {
  const response = await apiClient.get<Blob>(
    `/v1/tasks/${taskId}/attachments/${attachmentId}/download`,
    {
      responseType: 'blob',
    },
  )

  return {
    blob: response.data,
    fileName: readDownloadFileName(response.headers['content-disposition']),
    contentType: readHeaderValue(response.headers['content-type']),
  } satisfies DownloadTaskAttachmentResult
}

export async function deleteTaskAttachment(taskId: string, attachmentId: string) {
  await apiClient.delete(`/v1/tasks/${taskId}/attachments/${attachmentId}`)
}

function adaptTaskAttachmentListResponse(data: unknown): TaskAttachment[] {
  if (!Array.isArray(data)) {
    throw new Error('Invalid task attachment list response.')
  }

  return data.map(adaptTaskAttachmentResponse)
}

function adaptTaskAttachmentResponse(data: unknown): TaskAttachment {
  if (!isObject(data)) {
    throw new Error('Invalid task attachment response.')
  }

  const mediaKind = readRequiredString(data, 'mediaKind', 'task attachment')

  if (!isMediaKind(mediaKind)) {
    throw new Error('Invalid task attachment media kind response.')
  }

  return {
    id: readRequiredString(data, 'id', 'task attachment'),
    version: readNullableNumber(data, 'version'),
    taskId: readRequiredString(data, 'taskId', 'task attachment'),
    ownerUserId: readRequiredString(data, 'ownerUserId', 'task attachment'),
    objectKey: readRequiredString(data, 'objectKey', 'task attachment'),
    fileName: readRequiredString(data, 'fileName', 'task attachment'),
    contentType: readRequiredString(data, 'contentType', 'task attachment'),
    sizeBytes: readRequiredNumber(data, 'sizeBytes', 'task attachment'),
    mediaKind,
    deletedAt: readNullableString(data, 'deletedAt'),
    createdAt: readRequiredString(data, 'createdAt', 'task attachment'),
    updatedAt: readRequiredString(data, 'updatedAt', 'task attachment'),
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

function readNullableNumber(data: Record<string, unknown>, key: string) {
  const value = data[key]

  if (value === null || value === undefined) {
    return null
  }

  if (typeof value !== 'number' || !Number.isFinite(value)) {
    throw new Error(`Invalid response: ${key} must be a number or null.`)
  }

  return value
}

function isMediaKind(mediaKind: string): mediaKind is MediaKind {
  return ['IMAGE', 'DOCUMENT', 'AUDIO', 'VIDEO', 'OTHER'].includes(mediaKind)
}

function readDownloadFileName(value: unknown) {
  const headerValue = readHeaderValue(value)

  if (!headerValue) {
    return null
  }

  const encodedMatch = headerValue.match(/filename\*=UTF-8''([^;]+)/i)
  if (encodedMatch?.[1]) {
    return decodeURIComponent(encodedMatch[1])
  }

  const match = headerValue.match(/filename="?([^";]+)"?/i)
  return match?.[1] ?? null
}

function readHeaderValue(value: unknown) {
  if (Array.isArray(value)) {
    return value[0] ?? null
  }

  return typeof value === 'string' ? value : null
}
