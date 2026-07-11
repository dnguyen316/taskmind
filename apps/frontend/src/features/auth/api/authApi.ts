import { apiClient } from '../../../lib/apiClient'

export interface AuthTokensResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
}

export interface AuthUserResponse {
  userId: string
  email: string
  displayName: string
  onboardingCompleted: boolean
  onboardingWorkspaceType: string | null
  onboardingPlanningStyle: string | null
}

export interface LoginPayload {
  email: string
  password: string
}

export interface SignupEmailPayload extends LoginPayload {
  displayName: string
}

export interface VerifyOtpPayload {
  email: string
  otp: string
}

export type PasswordRecoveryFlow = 'reset-request' | 'reset-complete'

export interface PasswordResetRequestPayload {
  email: string
}

export interface PasswordResetCompletePayload {
  email: string
  token: string
  password: string
}

export type PasswordRecoveryPayload = PasswordResetRequestPayload | PasswordResetCompletePayload

export async function login(payload: LoginPayload) {
  const response = await apiClient.post<AuthTokensResponse>('/v1/auth/login', payload)
  return response.data
}

export async function signupEmail(payload: SignupEmailPayload) {
  await apiClient.post<void>('/v1/auth/signup/email', payload)
}

export async function verifyOtp(payload: VerifyOtpPayload) {
  const response = await apiClient.post<AuthTokensResponse>('/v1/auth/verify', payload)
  return response.data
}

export async function runPasswordFlow(
  flow: PasswordRecoveryFlow,
  payload: PasswordRecoveryPayload,
) {
  await apiClient.post<void>(`/v1/auth/password/${flow}`, payload)
}

export async function requestPasswordReset(payload: PasswordResetRequestPayload) {
  await runPasswordFlow('reset-request', payload)
}

export async function completePasswordReset(payload: PasswordResetCompletePayload) {
  await runPasswordFlow('reset-complete', payload)
}

export async function logout() {
  await apiClient.post<void>('/v1/auth/logout', {})
}

export async function getCurrentUser() {
  const response = await apiClient.get<AuthUserResponse>('/v1/auth/me')
  return response.data
}
