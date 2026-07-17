/** Formats a timestamp as "HH:mm:ss DD/MM/YY" in the viewer's local time.
 *  Accepts epoch millis (e.g. TanStack Query's dataUpdatedAt) or a Date.
 *  Returns an em dash for missing/invalid input so callers can render it raw. */
export function formatTimestamp(value: number | Date | null | undefined): string {
  if (value == null) return '—'
  const d = typeof value === 'number' ? new Date(value) : value
  if (Number.isNaN(d.getTime())) return '—'
  const pad = (n: number) => String(n).padStart(2, '0')
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  const date = `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${pad(d.getFullYear() % 100)}`
  return `${time} ${date}`
}
