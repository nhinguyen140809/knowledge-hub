import { useState } from 'react'
import { usePrincipalGraph } from '../../hooks/usePrincipals'
import type { Principal } from '../../types/access.type'
import type { MoveToGroupTarget } from './MoveToGroupDialog'
import type { RemoveMemberTarget } from './RemoveMemberDialog'

/** Everything transitively contained in `rootId` (not including it). Making
 *  any of these — or `rootId` itself — a parent of `rootId` closes a
 *  membership cycle, so the move and add-to-group dialogs keep them out of
 *  their target lists. The backend enforces the same rule authoritatively;
 *  this filter is UX, sparing the round trip. */
function descendantsOf(rootId: string, membership: Record<string, string[]>): Set<string> {
  const seen = new Set<string>()
  const queue = [rootId]
  while (queue.length > 0) {
    for (const child of membership[queue.pop()!] ?? []) {
      if (!seen.has(child)) {
        seen.add(child)
        queue.push(child)
      }
    }
  }
  return seen
}

/** Groups holding a direct membership edge to `id` — the "already in" set.
 *  Direct parents only, not the whole ancestor walk: joining a grandparent is
 *  a legitimate new edge in the DAG, but adding or moving into a group that
 *  already has the edge is a no-op add (and for move, a disguised remove). */
function directParentsOf(id: string, membership: Record<string, string[]>): Set<string> {
  return new Set(
    Object.entries(membership)
      .filter(([, memberIds]) => memberIds.includes(id))
      .map(([groupId]) => groupId),
  )
}

/**
 * The tree's whole model: the membership query, which dialog is open on which
 * target, and every derived list the rendering needs (roots, dialog
 * candidates, admin count). The component that consumes this only decides
 * what pixels each state turns into.
 */
export function usePrincipalTree() {
  const { data, isPending, isError, error } = usePrincipalGraph()
  const [deleteTarget, setDeleteTarget] = useState<Principal | null>(null)
  const [addMemberTarget, setAddMemberTarget] = useState<Principal | null>(null)
  const [addToGroupTarget, setAddToGroupTarget] = useState<Principal | null>(null)
  const [moveTarget, setMoveTarget] = useState<MoveToGroupTarget | null>(null)
  const [removeMemberTarget, setRemoveMemberTarget] = useState<RemoveMemberTarget | null>(null)

  const principals = data?.principals ?? []
  const membership = data?.membership ?? {}

  const byId = new Map(principals.map((p) => [p.principalId, p]))
  const adminCount = principals.filter((p) => p.role === 'ADMIN').length
  const hasParent = new Set(Object.values(membership).flat())
  const roots = principals.filter((p) => !hasParent.has(p.principalId))

  // A graph where every principal has a parent (a cycle with no entry point)
  // would leave no roots; show everything rather than an empty panel.
  const topLevel = roots.length > 0 ? roots : principals

  // Candidates for "add to group": every GROUP principal except the one being
  // added (can't contain itself), groups that already contain it directly (a
  // no-op), and anything nested inside it — a group joining its own
  // descendant would close a cycle. Existing memberships are untouched;
  // that's what distinguishes this from move.
  const addToGroupBlocked = addToGroupTarget
    ? descendantsOf(addToGroupTarget.principalId, membership)
    : new Set<string>()

  const addToGroupParents = addToGroupTarget
    ? directParentsOf(addToGroupTarget.principalId, membership)
    : new Set<string>()
  const addToGroupCandidates = principals.filter(
    (p) =>
      p.type === 'GROUP' &&
      p.principalId !== addToGroupTarget?.principalId &&
      !addToGroupParents.has(p.principalId) &&
      !addToGroupBlocked.has(p.principalId),
  )

  // Candidates for "move to group": every GROUP principal except the one
  // being moved (can't be its own parent), every group already directly
  // containing it (the add half would merge into an existing edge, turning
  // the move into a disguised remove — that covers `fromGroupId` too), and
  // anything nested inside it — moving into a descendant would close a cycle.
  const moveBlocked = moveTarget
    ? descendantsOf(moveTarget.principal.principalId, membership)
    : new Set<string>()
  const moveParents = moveTarget
    ? directParentsOf(moveTarget.principal.principalId, membership)
    : new Set<string>()
  const moveCandidates = principals.filter(
    (p) =>
      p.type === 'GROUP' &&
      p.principalId !== moveTarget?.principal.principalId &&
      !moveParents.has(p.principalId) &&
      !moveBlocked.has(p.principalId),
  )

  return {
    isPending,
    isError,
    error,
    isEmpty: principals.length === 0,
    byId,
    membership,
    adminCount,
    topLevel,
    deleteTarget,
    setDeleteTarget,
    addMemberTarget,
    setAddMemberTarget,
    addToGroupTarget,
    setAddToGroupTarget,
    addToGroupCandidates,
    moveTarget,
    setMoveTarget,
    removeMemberTarget,
    setRemoveMemberTarget,
    moveCandidates,
  }
}
