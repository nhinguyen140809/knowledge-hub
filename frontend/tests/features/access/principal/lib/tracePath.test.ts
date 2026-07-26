import { describe, expect, it } from 'vitest'
import { tracePath } from '@/features/access/lib/tracePath'
import type { AccessGraphEdge, PrincipalAccessGraph } from '@/features/access/types/access.type'

// Minimal graph builder: nodes are inferred from the edges, kinds don't affect
// tracePath (it works off edge kind and direction), so only edges matter here.
function graph(focus: string, edges: AccessGraphEdge[]): PrincipalAccessGraph {
  const ids = new Set<string>()
  for (const e of edges) {
    ids.add(e.from)
    ids.add(e.to)
  }
  return {
    focus,
    nodes: [...ids].map((id) => ({ id, kind: 'SUBJECT' })),
    edges,
  }
}

const member = (from: string, to: string): AccessGraphEdge => ({ from, to, kind: 'MEMBER' })
const grant = (from: string, to: string): AccessGraphEdge => ({ from, to, kind: 'GRANT' })

describe('tracePath', () => {
  it('traces a direct grant as the focus and the source alone', () => {
    const result = tracePath(graph('alice', [grant('alice', 'wiki')]), 'alice', 'wiki')

    expect([...result.principalIds]).toEqual(['alice'])
    expect([...result.sourceIds]).toEqual(['wiki'])
    expect([...result.edgeKeys]).toEqual(['GRANT:alice->wiki'])
  })

  it('traces an inherited grant through the granting group', () => {
    const result = tracePath(
      graph('alice', [member('eng', 'alice'), grant('eng', 'wiki')]),
      'alice',
      'wiki',
    )

    expect(result.principalIds).toEqual(new Set(['eng', 'alice']))
    expect(result.edgeKeys).toEqual(new Set(['MEMBER:eng->alice', 'GRANT:eng->wiki']))
  })

  it('includes intermediate groups between the holder and focus', () => {
    const result = tracePath(
      graph('alice', [member('all', 'eng'), member('eng', 'alice'), grant('all', 'wiki')]),
      'alice',
      'wiki',
    )

    // 'eng' sits between the grant holder 'all' and focus 'alice' — it must be
    // on the path even though it holds no grant itself.
    expect(result.principalIds).toEqual(new Set(['all', 'eng', 'alice']))
    expect(result.edgeKeys).toEqual(
      new Set(['MEMBER:all->eng', 'MEMBER:eng->alice', 'GRANT:all->wiki']),
    )
  })

  it('leaves an unrelated parallel group out of the path', () => {
    const result = tracePath(
      graph('alice', [member('eng', 'alice'), member('support', 'alice'), grant('eng', 'wiki')]),
      'alice',
      'wiki',
    )

    // 'support' is another of alice's groups but does not carry this grant.
    expect(result.principalIds).toEqual(new Set(['eng', 'alice']))
    expect(result.edgeKeys.has('MEMBER:support->alice')).toBe(false)
  })

  it('returns empty when no grant reaches the source', () => {
    const result = tracePath(graph('alice', [member('eng', 'alice')]), 'alice', 'wiki')

    expect(result.principalIds.size).toBe(0)
    expect(result.sourceIds.size).toBe(0)
    expect(result.edgeKeys.size).toBe(0)
  })
})
