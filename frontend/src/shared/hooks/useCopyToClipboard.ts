import { useCallback, useEffect, useState } from 'react'

/** How long `copied` stays true after a successful write before reverting, so a
 *  button can flash "Copied" and then offer to copy again on its own. */
const COPIED_RESET_MS = 1500

/**
 * Copy-to-clipboard with a self-resetting acknowledgement. `copied` flips true
 * on a successful write and reverts after {@link COPIED_RESET_MS}, so callers
 * get the "just copied" flash without owning a timer. Generalises the one-off
 * reveal-secret flow into something any id or value can reuse.
 */
export function useCopyToClipboard() {
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    if (!copied) return
    const timer = setTimeout(() => setCopied(false), COPIED_RESET_MS)
    return () => clearTimeout(timer)
  }, [copied])

  const copy = useCallback(async (text: string) => {
    await navigator.clipboard.writeText(text)
    setCopied(true)
  }, [])

  return { copied, copy }
}
