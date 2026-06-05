import { apiClient } from '../../../lib/apiClient'

export interface AuthTokensResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresInSeconds: number
}

export interface LoginPayload {
  email: string
  password: string
}

export interface SignupEmailPayload extends LoginPayload {
  displayName: string
}

export async function login(payload: LoginPayload) {
  const response = await apiClient.post<AuthTokensResponse>('/v1/auth/login', payload)
  return response.data
}

export async function signupEmail(payload: SignupEmailPayload) {
  const response = await apiClient.post<AuthTokensResponse>('/v1/auth/signup/email', payload)
  return response.data
}
