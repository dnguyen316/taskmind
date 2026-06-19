export interface TeamMember {
  userId: string
  displayName: string
  email: string
  openTasks: number
}

export interface TeamDirectory {
  members: TeamMember[]
  totalMembers: number
  totalOpenTasks: number
}
