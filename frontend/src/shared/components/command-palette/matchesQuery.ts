/**
 * Whether an item whose searchable text is `haystack` should show for the typed
 * `needle`. Both are expected already lowercased; `needle` must be non-empty.
 *
 * Subsequence match: every character of `needle` appears in `haystack` in order
 * but not necessarily adjacent, so "pe" finds "platform-engineering" and "sup"
 * finds "support-team" — forgiving of the hyphenated ids used here without
 * matching an arbitrary jumble of characters.
 */
export function matchesQuery(needle: string, haystack: string): boolean {
  let i = 0
  for (const char of haystack) {
    if (char === needle[i]) i++
    if (i === needle.length) return true
  }
  return false
}
