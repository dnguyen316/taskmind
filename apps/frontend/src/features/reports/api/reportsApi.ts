import jsPDF from 'jspdf'
import autoTable from 'jspdf-autotable'
import { apiClient } from '../../../lib/apiClient'
import type {
  ReportsAssigneeThroughput,
  ReportsAssigneeWorkload,
  ReportsDeltas,
  ReportsKpis,
  ReportsPrioritySegment,
  ReportsProjectThroughput,
  ReportsRange,
  ReportsRangeResponse,
  ReportsResponse,
  ReportsSparklines,
  ReportsStatusSegment,
  ReportsTeamWorkload,
  ReportsTrend,
} from '../types'

export async function getReports(range: ReportsRange) {
  const response = await apiClient.get<unknown>('/v1/reports', { params: { range } })
  return adaptReportsResponse(response.data)
}

function adaptReportsResponse(data: unknown): ReportsResponse {
  if (!isObject(data)) throw new Error('Invalid reports response.')
  const range = readRequiredString(data, 'range', 'reports')
  if (!isReportsRangeResponse(range)) throw new Error('Invalid reports response: unknown range.')
  return {
    range,
    kpis: adaptKpis(data.kpis),
    deltas: adaptDeltas(data.deltas),
    sparklines: adaptSparklines(data.sparklines),
    trends: readArray(data.trends, adaptTrend, 'reports trends'),
    statusSegments: readArray(data.statusSegments, adaptStatusSegment, 'reports status segments'),
    prioritySegments: readArray(
      data.prioritySegments,
      adaptPrioritySegment,
      'reports priority segments',
    ),
    projectThroughput: readArray(
      data.projectThroughput,
      adaptProjectThroughput,
      'reports project throughput',
    ),
    assigneeThroughput: readArray(
      data.assigneeThroughput,
      adaptAssigneeThroughput,
      'reports assignee throughput',
    ),
    assigneeWorkload: readArray(
      data.assigneeWorkload,
      adaptAssigneeWorkload,
      'reports assignee workload',
    ),
    teamWorkload: adaptTeamWorkload(data.teamWorkload),
  }
}

function adaptKpis(data: unknown): ReportsKpis {
  if (!isObject(data)) throw new Error('Invalid reports KPIs response.')
  return {
    tasksCreated: readRequiredNumber(data, 'tasksCreated', 'reports KPIs'),
    tasksCompleted: readRequiredNumber(data, 'tasksCompleted', 'reports KPIs'),
    projectsCreated: readRequiredNumber(data, 'projectsCreated', 'reports KPIs'),
    eventsIngested: readRequiredNumber(data, 'eventsIngested', 'reports KPIs'),
    completionRate: readRequiredNumber(data, 'completionRate', 'reports KPIs'),
  }
}

function adaptDeltas(data: unknown): ReportsDeltas {
  if (!isObject(data)) throw new Error('Invalid reports deltas response.')
  return {
    tasksCreated: readRequiredNumber(data, 'tasksCreated', 'reports deltas'),
    tasksCompleted: readRequiredNumber(data, 'tasksCompleted', 'reports deltas'),
    eventsIngested: readRequiredNumber(data, 'eventsIngested', 'reports deltas'),
  }
}

function adaptSparklines(data: unknown): ReportsSparklines {
  if (!isObject(data)) throw new Error('Invalid reports sparklines response.')
  return {
    tasksCreated: readNumberArray(data.tasksCreated, 'reports tasks-created sparkline'),
    tasksCompleted: readNumberArray(data.tasksCompleted, 'reports tasks-completed sparkline'),
    eventsIngested: readNumberArray(data.eventsIngested, 'reports events sparkline'),
  }
}

function adaptTrend(data: unknown): ReportsTrend {
  if (!isObject(data)) throw new Error('Invalid reports trend response.')
  return {
    date: readRequiredString(data, 'date', 'reports trend'),
    tasksCreated: readRequiredNumber(data, 'tasksCreated', 'reports trend'),
    tasksCompleted: readRequiredNumber(data, 'tasksCompleted', 'reports trend'),
    eventsIngested: readRequiredNumber(data, 'eventsIngested', 'reports trend'),
  }
}

function adaptStatusSegment(data: unknown): ReportsStatusSegment {
  if (!isObject(data)) throw new Error('Invalid reports status segment response.')
  return {
    status: readRequiredString(data, 'status', 'reports status segment'),
    count: readRequiredNumber(data, 'count', 'reports status segment'),
  }
}

function adaptPrioritySegment(data: unknown): ReportsPrioritySegment {
  if (!isObject(data)) throw new Error('Invalid reports priority segment response.')
  return {
    priority: readRequiredString(data, 'priority', 'reports priority segment'),
    count: readRequiredNumber(data, 'count', 'reports priority segment'),
  }
}

function adaptProjectThroughput(data: unknown): ReportsProjectThroughput {
  if (!isObject(data)) throw new Error('Invalid reports project throughput response.')
  return {
    projectId: readRequiredString(data, 'projectId', 'reports project throughput'),
    name: readRequiredString(data, 'name', 'reports project throughput'),
    tasksCreated: readRequiredNumber(data, 'tasksCreated', 'reports project throughput'),
    tasksCompleted: readRequiredNumber(data, 'tasksCompleted', 'reports project throughput'),
  }
}

function adaptAssigneeThroughput(data: unknown): ReportsAssigneeThroughput {
  if (!isObject(data)) throw new Error('Invalid reports assignee throughput response.')
  return {
    userId: readRequiredString(data, 'userId', 'reports assignee throughput'),
    tasksCreated: readRequiredNumber(data, 'tasksCreated', 'reports assignee throughput'),
    tasksCompleted: readRequiredNumber(data, 'tasksCompleted', 'reports assignee throughput'),
  }
}

function adaptAssigneeWorkload(data: unknown): ReportsAssigneeWorkload {
  if (!isObject(data)) throw new Error('Invalid reports assignee workload response.')
  return {
    userId: readRequiredString(data, 'userId', 'reports assignee workload'),
    openTasks: readRequiredNumber(data, 'openTasks', 'reports assignee workload'),
  }
}

function adaptTeamWorkload(data: unknown): ReportsTeamWorkload {
  if (!isObject(data)) throw new Error('Invalid reports team workload response.')
  return {
    members: readRequiredNumber(data, 'members', 'reports team workload'),
    openTasks: readRequiredNumber(data, 'openTasks', 'reports team workload'),
  }
}

function isReportsRangeResponse(value: string): value is ReportsRangeResponse {
  return ['WEEK', 'MONTH', 'QUARTER'].includes(value)
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function readArray<T>(data: unknown, adapt: (item: unknown) => T, resourceName: string) {
  if (!Array.isArray(data)) throw new Error(`Invalid ${resourceName} response.`)
  return data.map(adapt)
}

function readNumberArray(data: unknown, resourceName: string) {
  if (
    !Array.isArray(data) ||
    data.some((item) => typeof item !== 'number' || !Number.isFinite(item))
  ) {
    throw new Error(`Invalid ${resourceName} response.`)
  }
  return data
}

function readRequiredString(data: Record<string, unknown>, key: string, resourceName: string) {
  const value = data[key]
  if (typeof value !== 'string' || value.length === 0)
    throw new Error(`Invalid ${resourceName} response: missing ${key}.`)
  return value
}

function readRequiredNumber(data: Record<string, unknown>, key: string, resourceName: string) {
  const value = data[key]
  if (typeof value !== 'number' || !Number.isFinite(value))
    throw new Error(`Invalid ${resourceName} response: missing ${key}.`)
  return value
}

export function exportReportsPdf(report: ReportsResponse) {
  const doc = new jsPDF()
  doc.setFontSize(18)
  doc.text(`TaskMind ${report.range.toLowerCase()} report`, 14, 18)
  doc.setFontSize(10)
  doc.text(`Completion rate: ${Math.round(report.kpis.completionRate * 100)}%`, 14, 26)

  autoTable(doc, {
    startY: 34,
    head: [['KPI', 'Value']],
    body: [
      ['Tasks created', report.kpis.tasksCreated],
      ['Tasks completed', report.kpis.tasksCompleted],
      ['Projects created', report.kpis.projectsCreated],
      ['Events ingested', report.kpis.eventsIngested],
    ],
  })

  autoTable(doc, {
    head: [['Project', 'Created', 'Completed']],
    body: report.projectThroughput.map((row) => [row.name, row.tasksCreated, row.tasksCompleted]),
  })

  autoTable(doc, {
    head: [['Assignee', 'Open tasks']],
    body: report.assigneeWorkload.map((row) => [row.userId, row.openTasks]),
  })

  doc.save(`taskmind-${report.range.toLowerCase()}-report.pdf`)
}
