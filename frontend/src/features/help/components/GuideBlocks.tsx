import type { ReactNode } from 'react'

/** Ordered steps for a walkthrough. */
export function Steps({ children }: { children: ReactNode }) {
  return <ol className="text-muted list-decimal space-y-1.5 pl-5 text-sm">{children}</ol>
}

/** A standalone note that is not a numbered step. */
export function Note({ children }: { children: ReactNode }) {
  return <p className="text-muted text-sm">{children}</p>
}
