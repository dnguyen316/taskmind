import type { CalendarViewMode, ScheduledBlock } from '../types'

export const MINUTES_IN_DAY = 24 * 60
export const HOUR_MARKS = Array.from({ length: 24 }, (_, hour) => hour)

export interface CalendarMonthDay {
  date: Date
  isCurrentMonth: boolean
  isToday: boolean
}

export function startOfDay(date: Date) {
  const next = new Date(date)
  next.setHours(0, 0, 0, 0)
  return next
}

export function endOfDay(date: Date) {
  const next = new Date(date)
  next.setHours(23, 59, 59, 999)
  return next
}

export function startOfWeek(date: Date) {
  const next = startOfDay(date)
  next.setDate(next.getDate() - next.getDay())
  return next
}

export function endOfWeek(date: Date) {
  const next = startOfWeek(date)
  next.setDate(next.getDate() + 6)
  return endOfDay(next)
}

export function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

export function endOfMonth(date: Date) {
  return endOfDay(new Date(date.getFullYear(), date.getMonth() + 1, 0))
}

export function addDays(date: Date, days: number) {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

export function addPeriods(date: Date, mode: CalendarViewMode, direction: 1 | -1) {
  const next = new Date(date)

  if (mode === 'day') next.setDate(next.getDate() + direction)
  if (mode === 'week') next.setDate(next.getDate() + 7 * direction)
  if (mode === 'month') next.setMonth(next.getMonth() + direction)

  return next
}

export function getVisibleRange(date: Date, mode: CalendarViewMode) {
  if (mode === 'day') return { from: startOfDay(date), to: endOfDay(date) }
  if (mode === 'week') return { from: startOfWeek(date), to: endOfWeek(date) }

  const monthStart = startOfMonth(date)
  const monthEnd = endOfMonth(date)
  return { from: startOfWeek(monthStart), to: endOfWeek(monthEnd) }
}

export function toSchedulerRangeParams(range: { from: Date; to: Date }) {
  return {
    from: range.from.toISOString(),
    to: range.to.toISOString(),
  }
}

export function buildMonthGrid(date: Date) {
  const monthStart = startOfMonth(date)
  const gridStart = startOfWeek(monthStart)
  const gridEnd = endOfWeek(endOfMonth(date))
  const days: CalendarMonthDay[] = []

  for (let cursor = gridStart; cursor <= gridEnd; cursor = addDays(cursor, 1)) {
    days.push({
      date: new Date(cursor),
      isCurrentMonth: cursor.getMonth() === date.getMonth(),
      isToday: isSameDay(cursor, new Date()),
    })
  }

  return days
}

export function daysBetweenInclusive(from: Date, to: Date) {
  const days: Date[] = []
  for (let cursor = startOfDay(from); cursor <= to; cursor = addDays(cursor, 1)) {
    days.push(new Date(cursor))
  }
  return days
}

export function isSameDay(left: Date, right: Date) {
  return (
    left.getFullYear() === right.getFullYear() &&
    left.getMonth() === right.getMonth() &&
    left.getDate() === right.getDate()
  )
}

export function blocksForDay(blocks: ScheduledBlock[], day: Date) {
  return blocks.filter((block) => isSameDay(new Date(block.startsAt), day))
}

export function minuteOffset(value: string | Date) {
  const date = typeof value === 'string' ? new Date(value) : value
  return date.getHours() * 60 + date.getMinutes()
}

export function blockDurationMinutes(block: ScheduledBlock) {
  return Math.max(
    15,
    Math.round((new Date(block.endsAt).getTime() - new Date(block.startsAt).getTime()) / 60_000),
  )
}

export function formatVisibleRangeTitle(date: Date, mode: CalendarViewMode) {
  const range = getVisibleRange(date, mode)
  if (mode === 'day') {
    return date.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })
  }
  if (mode === 'month') {
    return date.toLocaleDateString(undefined, { month: 'long', year: 'numeric' })
  }

  return `${range.from.toLocaleDateString(undefined, { month: 'short', day: 'numeric' })} – ${range.to.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}`
}
