export const DEFAULT_USER_ID = '00000000-0000-0000-0000-000000000001'

export const TASK_STATUS_OPTIONS = [
  'TODO',
  'IN_PROGRESS',
  'DONE',
  'BLOCKED',
  'ARCHIVED',
]

export const STATUS_TRANSITIONS = [
  { label: 'Start', value: 'IN_PROGRESS' },
  { label: 'Done', value: 'DONE' },
  { label: 'Block', value: 'BLOCKED' },
]

export const DEFAULT_CREATE_TASK_FORM = Object.freeze({
  title: '',
  description: '',
  priority: 3,
  durationMinutes: 30,
  dueAt: '',
  status: 'TODO',
})
