import { createContext, useContext } from 'react'
import type { Principal } from '../../types/access.type'
import type { MoveToGroupTarget } from './MoveToGroupDialog'
import type { RemoveMemberTarget } from './RemoveMemberDialog'

/** Everything identical for every row of the tree. The recursive node reads
 *  this from context so the recursion only threads what actually varies per
 *  node (principal, path, parent) instead of re-passing eight unchanged
 *  props at every level. */
export interface PrincipalTreeContextValue {
  byId: Map<string, Principal>
  membership: Record<string, string[]>
  /** How many ADMIN principals exist — the last one must not be deletable. */
  adminCount: number
  selectedId?: string | null
  onSelect?: (principalId: string) => void
  requestDelete: (principal: Principal) => void
  requestAddMember: (group: Principal) => void
  requestAddToGroup: (principal: Principal) => void
  requestMoveToGroup: (target: MoveToGroupTarget) => void
  requestRemoveMember: (target: RemoveMemberTarget) => void
}

export const PrincipalTreeContext = createContext<PrincipalTreeContextValue | null>(null)

export function usePrincipalTreeContext(): PrincipalTreeContextValue {
  const value = useContext(PrincipalTreeContext)
  if (!value) throw new Error('usePrincipalTreeContext must be used inside PrincipalTree')
  return value
}
