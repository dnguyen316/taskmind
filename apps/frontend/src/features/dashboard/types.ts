export interface DashboardKpis {
  openTasks: number
  completedTasks: number
  eventsIngested: number
  completionRate: number
}
export interface DashboardTaskItem {
  taskId: string
  projectId: string | null
  title: string
  status: string
  updatedAt: string
}
export interface DashboardActivitySnippet {
  date: string
  tasksCreated: number
  tasksCompleted: number
  eventsIngested: number
}
export interface DashboardResponse {
  kpis: DashboardKpis
  myTasks: DashboardTaskItem[]
  activity: DashboardActivitySnippet[]
}
