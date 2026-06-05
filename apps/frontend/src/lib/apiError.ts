export interface ApiErrorShape {
  status: number | null
  message: string
  path: string | null
  traceId: string | null
  retryable: boolean
  details: unknown
}

export class ApiError extends Error implements ApiErrorShape {
  status: number | null
  path: string | null
  traceId: string | null
  retryable: boolean
  details: unknown

  constructor({ status, message, path, traceId, retryable, details }: ApiErrorShape) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.path = path
    this.traceId = traceId
    this.retryable = retryable
    this.details = details
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError
}
