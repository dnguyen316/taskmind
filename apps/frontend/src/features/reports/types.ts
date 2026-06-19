export type ReportsRange = 'week' | 'month' | 'quarter'
export type ReportsRangeResponse = 'WEEK' | 'MONTH' | 'QUARTER'

export interface ReportsKpis {
  tasksCreated: number
  tasksCompleted: number
  projectsCreated: number
  eventsIngested: number
  completionRate: number
}

export interface ReportsDeltas {
  tasksCreated: number
  tasksCompleted: number
  eventsIngested: number
}

export interface ReportsSparklines {
  tasksCreated: number[]
  tasksCompleted: number[]
  eventsIngested: number[]
}

export interface ReportsTrend {
  date: string
  tasksCreated: number
  tasksCompleted: number
  eventsIngested: number
}

export interface ReportsStatusSegment {
  status: string
  count: number
}

export interface ReportsPrioritySegment {
  priority: string
  count: number
}

export interface ReportsProjectThroughput {
  projectId: string
  name: string
  tasksCreated: number
  tasksCompleted: number
}

export interface ReportsAssigneeThroughput {
  userId: string
  tasksCreated: number
  tasksCompleted: number
}

export interface ReportsAssigneeWorkload {
  userId: string
  openTasks: number
}

export interface ReportsTeamWorkload {
  members: number
  openTasks: number
}

export interface ReportsResponse {
  range: ReportsRangeResponse
  kpis: ReportsKpis
  deltas: ReportsDeltas
  sparklines: ReportsSparklines
  trends: ReportsTrend[]
  statusSegments: ReportsStatusSegment[]
  prioritySegments: ReportsPrioritySegment[]
  projectThroughput: ReportsProjectThroughput[]
  assigneeThroughput: ReportsAssigneeThroughput[]
  assigneeWorkload: ReportsAssigneeWorkload[]
  teamWorkload: ReportsTeamWorkload
}
