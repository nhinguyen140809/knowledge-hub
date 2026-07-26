import { describe, expect, it } from 'vitest'
import {
  grantEdge,
  memberEdge,
  principalNode,
  sourceNode,
} from '@/features/access/lib/accessGraphElements'
import { sourceNodeId } from '@/features/access/lib/sourceNode'

describe('principalNode', () => {
  it('labels a group with a "(group)" suffix, a subject without one', () => {
    expect(principalNode('eng-team', 'GROUP', false).data).toMatchObject({
      label: 'eng-team (group)',
      variant: 'accent',
      isSelected: false,
    })
    expect(principalNode('bob', 'SUBJECT', true).data).toMatchObject({
      label: 'bob',
      variant: 'neutral',
      isSelected: true,
    })
  })
})

describe('sourceNode', () => {
  it('namespaces the id and labels it with the plain source id', () => {
    const node = sourceNode('engineering-wiki')
    expect(node.id).toBe(sourceNodeId('engineering-wiki'))
    expect(node.data).toMatchObject({ label: 'engineering-wiki', variant: 'success' })
  })
})

describe('memberEdge', () => {
  it('connects the two ids directly, unprefixed', () => {
    const edge = memberEdge('eng-team', 'bob')
    expect(edge.source).toBe('eng-team')
    expect(edge.target).toBe('bob')
  })

  it('fades the line and drops the arrowhead when faded', () => {
    const edge = memberEdge('eng-team', 'bob', 'faded')
    expect(edge.markerEnd).toBeUndefined()
    expect(edge.style).toMatchObject({ opacity: 0.15 })
  })

  it('thickens and raises z-index when traced', () => {
    const plain = memberEdge('eng-team', 'bob', 'plain')
    const traced = memberEdge('eng-team', 'bob', 'traced')
    expect(traced.zIndex).toBe(10)
    expect((traced.style as { strokeWidth: number }).strokeWidth).toBeGreaterThan(
      (plain.style as { strokeWidth: number }).strokeWidth,
    )
  })
})

describe('grantEdge', () => {
  it('targets the namespaced source node id, not the plain source id', () => {
    const edge = grantEdge('eng-team', 'engineering-wiki')
    expect(edge.source).toBe('eng-team')
    expect(edge.target).toBe(sourceNodeId('engineering-wiki'))
  })
})
