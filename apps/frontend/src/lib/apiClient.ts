import axios, { AxiosError } from 'axios'
import { getAuthorizationHeader } from './authToken'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  const authorizationHeader = getAuthorizationHeader()

  if (authorizationHeader) {
    config.headers.Authorization = authorizationHeader
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<{ message?: string }>) => {
    if (error.response) {
      const method = error.config?.method?.toUpperCase() ?? 'GET'
      const url = error.config?.url ?? ''
      const backendMessage = error.response.data?.message
      const statusMessage = `${method} ${url} failed (${error.response.status})`

      throw new Error(backendMessage || statusMessage)
    }

    if (error.code === 'ECONNABORTED') {
      throw new Error('The request timed out. Please try again.')
    }

    if (error.request) {
      throw new Error('Unable to reach the server. Check your connection and try again.')
    }

    throw new Error(error.message || 'Unexpected API error.')
  },
)
