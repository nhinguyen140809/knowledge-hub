import { describe, expect, it } from 'vitest'
import { formatTimestamp } from '@/shared/lib/datetime.utils'

describe('formatTimestamp', () => {
  it('formats a Date as HH:mm:ss DD/MM/YY, zero-padded', () => {
    // Constructed from local parts and read back with local getters, so the
    // result is timezone-independent.
    expect(formatTimestamp(new Date(2026, 6, 17, 9, 5, 3))).toBe('09:05:03 17/07/26')
    expect(formatTimestamp(new Date(2026, 10, 1, 23, 59, 0))).toBe('23:59:00 01/11/26')
  })

  it('returns an em dash for missing or invalid input', () => {
    expect(formatTimestamp(null)).toBe('—')
    expect(formatTimestamp(undefined)).toBe('—')
    expect(formatTimestamp(new Date('nonsense'))).toBe('—')
  })
})
