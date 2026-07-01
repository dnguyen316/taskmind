export type IntegrationProvider = 'GITHUB' | 'JIRA' | 'WIKI'
export type GitHubAllowedOperation =
  | 'READ_ISSUES'
  | 'READ_CODE'
  | 'CREATE_BRANCH'
  | 'CREATE_PR'
  | 'COMMENT'

export interface IntegrationConnection {
  id: string
  provider: IntegrationProvider
  accountId: string | null
  accountName: string | null
  displayName: string | null
  connectedAt: string | null
}

export interface IntegrationProjectLink {
  id: string
  projectId: string
  connectionId: string
  provider: IntegrationProvider
  externalProjectId: string | null
  externalProjectKey: string | null
  externalProjectName: string | null
  metadataJson: string | null
  repositoryOwner: string | null
  repositoryName: string | null
  defaultBranch: string | null
  installationId: string | null
  accountId: string | null
  allowedOperationsJson: string | null
}

export interface GitHubRepository {
  id: string
  owner: string
  name: string
  fullName: string
  defaultBranch: string
  isPrivate: boolean
  htmlUrl: string
  installationId: string | null
  accountId: string | null
}

export interface GitHubRepositoryLinkPayload {
  connectionId: string
  owner: string
  repo: string
  allowedOperations: GitHubAllowedOperation[]
}
