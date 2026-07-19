import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import type { PrincipalGraph } from '@/features/access/types/access.type'

const graphResult = vi.hoisted(() => ({ current: {} as Record<string, unknown> }))

// Stub the data hook so the tree can be driven with hand-built graphs — no query
// client, store or network involved.
vi.mock('@/features/access/hooks/usePrincipals', () => ({
  usePrincipalGraph: () => graphResult.current,
}))

const { PrincipalTree } = await import('@/features/access/components/PrincipalTree')

function renderGraph(graph: PrincipalGraph) {
  graphResult.current = { data: graph, isPending: false, isError: false, error: null }
  return render(<PrincipalTree />)
}

describe('PrincipalTree', () => {
  it('nests groups and only shows rootless principals at the top', () => {
    renderGraph({
      principals: [
        { principalId: 'eng-team', type: 'GROUP', role: 'MEMBER' },
        { principalId: 'alice', type: 'SUBJECT', role: 'ADMIN' },
      ],
      membership: { 'eng-team': ['alice'] },
    })

    // alice has a parent, so she appears once — nested, not at the root.
    expect(screen.getAllByText('alice')).toHaveLength(1)
    expect(screen.getByText('eng-team')).toBeInTheDocument()
  })

  it('renders a principal once per group it belongs to', () => {
    renderGraph({
      principals: [
        { principalId: 'eng-team', type: 'GROUP', role: 'MEMBER' },
        { principalId: 'support-team', type: 'GROUP', role: 'MEMBER' },
        { principalId: 'carol', type: 'SUBJECT', role: 'MEMBER' },
      ],
      membership: { 'eng-team': ['carol', 'support-team'], 'support-team': ['carol'] },
    })

    // Membership is a DAG: carol is reachable through both groups.
    expect(screen.getAllByText('carol')).toHaveLength(2)
  })

  it('cuts recursion when a cycle repeats a principal on the current path', () => {
    // a -> b -> a. Without the ancestor guard this would recurse forever.
    renderGraph({
      principals: [
        { principalId: 'a', type: 'GROUP', role: 'MEMBER' },
        { principalId: 'b', type: 'GROUP', role: 'MEMBER' },
      ],
      membership: { a: ['b'], b: ['a'] },
    })

    // Rendering terminating at all is the assertion; a cycle with no entry point
    // also has no roots, so both principals are shown and each cut is marked.
    expect(screen.getAllByText('cycle').length).toBeGreaterThan(0)
    expect(screen.getAllByText('a').length).toBeGreaterThan(0)
  })
})
