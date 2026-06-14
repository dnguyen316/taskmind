import { ref } from 'vue'
import { apiClient } from '../../../lib/apiClient'
import type {
  CaptureAcceptRequest,
  CaptureAcceptResponse,
  CaptureRejectRequest,
  CaptureRejectResponse,
  CaptureResponse,
} from './types'

export function useCapture() {
  const loading = ref(false)
  const accepting = ref(false)
  const rejecting = ref(false)
  const result = ref<CaptureResponse | null>(null)

  async function capture(text: string) {
    loading.value = true
    try {
      const response = await apiClient.post<CaptureResponse>('/v1/ai/capture', { text })
      result.value = response.data
      return response.data
    } finally {
      loading.value = false
    }
  }

  async function acceptDraft(request: CaptureAcceptRequest) {
    accepting.value = true
    try {
      const response = await apiClient.post<CaptureAcceptResponse>('/v1/ai/capture/accept', request)
      return response.data
    } finally {
      accepting.value = false
    }
  }

  async function rejectDraft(request: CaptureRejectRequest) {
    rejecting.value = true
    try {
      const response = await apiClient.post<CaptureRejectResponse>('/v1/ai/capture/reject', request)
      return response.data
    } finally {
      rejecting.value = false
    }
  }

  return { loading, accepting, rejecting, result, capture, acceptDraft, rejectDraft }
}
