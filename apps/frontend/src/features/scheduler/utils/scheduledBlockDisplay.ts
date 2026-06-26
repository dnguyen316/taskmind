import type { ScheduledBlock, ScheduledBlockStatus } from '../types'

export function statusColor(status: ScheduledBlockStatus) {
  if (status === 'COMPLETED') return 'green'
  if (status === 'MISSED') return 'orange'
  if (status === 'CANCELLED') return 'default'
  return 'blue'
}

export function statusLabel(status: ScheduledBlockStatus) {
  return status.replace('_', ' ')
}

export function formatRange(block: ScheduledBlock) {
  return `${new Date(block.startsAt).toLocaleString()} → ${new Date(block.endsAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

export function durationLabel(block: ScheduledBlock) {
  const durationMinutes = Math.round(
    (new Date(block.endsAt).getTime() - new Date(block.startsAt).getTime()) / 60_000,
  )
  return `${Math.max(durationMinutes, 0)} min`
}

export function compactTimeRange(block: ScheduledBlock) {
  return `${new Date(block.startsAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} – ${new Date(block.endsAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

export function toDateTimeLocal(value: string) {
  const date = new Date(value)
  const timezoneOffsetMs = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - timezoneOffsetMs).toISOString().slice(0, 16)
}

export function fromDateTimeLocal(value: string) {
  return new Date(value).toISOString()
}
