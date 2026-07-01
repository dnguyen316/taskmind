import { apiClient } from '../../../lib/apiClient'
import type {
  GitHubRepository,
  GitHubRepositoryLinkPayload,
  IntegrationConnection,
  IntegrationProjectLink,
  IntegrationProvider,
} from '../types'

export async function listConnections() {
  const response = await apiClient.get<unknown[]>('/v1/integrations/connections')
  return response.data.map(adaptConnection)
}

export async function listProjectLinks(projectId: string) {
  const response = await apiClient.get<unknown[]>(`/v1/integrations/projects/${projectId}/links`)
  return response.data.map(adaptProjectLink)
}

export async function discoverGitHubRepository(connectionId: string, owner: string, repo: string) {
  const response = await apiClient.get<unknown>(
    `/v1/integrations/github/repositories/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}`,
    { params: { connectionId } },
  )
  return adaptGitHubRepository(response.data)
}

export async function linkGitHubRepository(
  projectId: string,
  payload: GitHubRepositoryLinkPayload,
) {
  const response = await apiClient.post<unknown>(
    `/v1/integrations/github/projects/${projectId}/repositories`,
    payload,
  )
  return adaptProjectLink(response.data)
}

function adaptConnection(data: unknown): IntegrationConnection {
  const value = asRecord(data, 'integration connection')
  return {
    id: readString(value, 'id'),
    provider: readString(value, 'provider') as IntegrationProvider,
    accountId: readNullableString(value, 'accountId'),
    accountName: readNullableString(value, 'accountName'),
    displayName: readNullableString(value, 'displayName'),
    connectedAt: readNullableString(value, 'connectedAt'),
  }
}

function adaptProjectLink(data: unknown): IntegrationProjectLink {
  const value = asRecord(data, 'project link')
  return {
    id: readString(value, 'id'),
    projectId: readString(value, 'projectId'),
    connectionId: readString(value, 'connectionId'),
    provider: readString(value, 'provider') as IntegrationProvider,
    externalProjectId: readNullableString(value, 'externalProjectId'),
    externalProjectKey: readNullableString(value, 'externalProjectKey'),
    externalProjectName: readNullableString(value, 'externalProjectName'),
    metadataJson: readNullableString(value, 'metadataJson'),
    repositoryOwner: readNullableString(value, 'repositoryOwner'),
    repositoryName: readNullableString(value, 'repositoryName'),
    defaultBranch: readNullableString(value, 'defaultBranch'),
    installationId: readNullableString(value, 'installationId'),
    accountId: readNullableString(value, 'accountId'),
    allowedOperationsJson: readNullableString(value, 'allowedOperationsJson'),
  }
}

function adaptGitHubRepository(data: unknown): GitHubRepository {
  const value = asRecord(data, 'GitHub repository')
  return {
    id: readString(value, 'id'),
    owner: readString(value, 'owner'),
    name: readString(value, 'name'),
    fullName: readString(value, 'fullName'),
    defaultBranch: readString(value, 'defaultBranch'),
    isPrivate: Boolean(value.isPrivate),
    htmlUrl: readString(value, 'htmlUrl'),
    installationId: readNullableString(value, 'installationId'),
    accountId: readNullableString(value, 'accountId'),
  }
}

function asRecord(value: unknown, name: string): Record<string, unknown> {
  if (typeof value !== 'object' || value === null) throw new Error(`Invalid ${name} response.`)
  return value as Record<string, unknown>
}
function readString(value: Record<string, unknown>, key: string) {
  const field = value[key]
  if (typeof field !== 'string') throw new Error(`Invalid response: missing ${key}.`)
  return field
}
function readNullableString(value: Record<string, unknown>, key: string) {
  const field = value[key]
  return typeof field === 'string' ? field : null
}
