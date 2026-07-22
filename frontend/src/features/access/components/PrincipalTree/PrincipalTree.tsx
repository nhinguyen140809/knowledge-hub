import { Skeleton } from '@heroui/react'
import { Users } from 'lucide-react'
import { useState } from 'react'
import { EmptyState } from '@/shared/components/ui/EmptyState'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { Tree } from '@/shared/components/ui/Tree'
import { AddMemberDialog } from './AddMemberDialog'
import { DeletePrincipalDialog } from './DeletePrincipalDialog'
import { MoveToGroupDialog, type MoveToGroupTarget } from './MoveToGroupDialog'
import { PrincipalTreeNode } from './PrincipalTreeNode'
import { RemoveMemberDialog, type RemoveMemberTarget } from './RemoveMemberDialog'
import { useDeletePrincipal, useRemoveMember } from '../../hooks/usePrincipalMutations'
import { usePrincipalGraph } from '../../hooks/usePrincipals'
import type { Principal } from '../../types/access.type'

interface PrincipalTreeProps {
  selectedId?: string | null
  onSelect?: (principalId: string) => void
  /** Fires after a principal is successfully deleted — the owner of the
   *  selection needs it to drop a selection that now points at nothing. */
  onDeleted?: (principalId: string) => void
}

/**
 * The principal hierarchy. Membership is a directed graph, not a tree: groups
 * nest, a principal can belong to several groups, and the backend does not
 * reject cycles.
 */
export function PrincipalTree({ selectedId, onSelect, onDeleted }: PrincipalTreeProps) {
  const { data, isPending, isError, error } = usePrincipalGraph()
  const [deleteTarget, setDeleteTarget] = useState<Principal | null>(null)
  const [addMemberTarget, setAddMemberTarget] = useState<Principal | null>(null)
  const [moveTarget, setMoveTarget] = useState<MoveToGroupTarget | null>(null)
  const [removeMemberTarget, setRemoveMemberTarget] = useState<RemoveMemberTarget | null>(null)
  const deletePrincipal = useDeletePrincipal()
  const removeMember = useRemoveMember()

  if (isPending) {
    return (
      <div className="flex flex-col gap-2">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-8 w-full rounded-lg" />
        ))}
      </div>
    )
  }

  if (isError) return <ErrorState description={(error as Error).message} />

  if (data.principals.length === 0) {
    return <EmptyState icon={<Users size={28} />} description="No principals yet." />
  }

  const byId = new Map(data.principals.map((p) => [p.principalId, p]))
  const membership = data.membership
  const hasParent = new Set(Object.values(membership).flat())
  const roots = data.principals.filter((p) => !hasParent.has(p.principalId))

  // A graph where every principal has a parent (a cycle with no entry point)
  // would leave no roots; show everything rather than an empty panel.
  const topLevel = roots.length > 0 ? roots : data.principals

  // Candidates for "add member": every principal except the group itself and
  // whoever is already a direct member of it.
  const currentMembers = new Set(
    addMemberTarget ? (membership[addMemberTarget.principalId] ?? []) : [],
  )
  const addMemberCandidates = data.principals.filter(
    (p) => p.principalId !== addMemberTarget?.principalId && !currentMembers.has(p.principalId),
  )

  // Candidates for "move to group": every GROUP principal except the one
  // being moved (can't be its own parent) and its current parent (a no-op).
  const moveCandidates = data.principals.filter(
    (p) =>
      p.type === 'GROUP' &&
      p.principalId !== moveTarget?.principal.principalId &&
      p.principalId !== moveTarget?.fromGroupId,
  )

  return (
    <>
      <Tree>
        {topLevel.map((p) => (
          <PrincipalTreeNode
            key={p.principalId}
            principal={p}
            path={[p.principalId]}
            byId={byId}
            membership={membership}
            selectedId={selectedId}
            onSelect={onSelect}
            onDeleteRequest={setDeleteTarget}
            onAddMemberRequest={setAddMemberTarget}
            onMoveToGroupRequest={setMoveTarget}
            onRemoveMemberRequest={setRemoveMemberTarget}
          />
        ))}
      </Tree>

      <DeletePrincipalDialog
        target={deleteTarget}
        onOpenChange={(isOpen) => !isOpen && setDeleteTarget(null)}
        isPending={deletePrincipal.isPending}
        onConfirm={() => {
          if (!deleteTarget) return
          // Captured before mutating: the dialog closes (clearing deleteTarget)
          // while the request is still in flight.
          const id = deleteTarget.principalId
          deletePrincipal.mutate(id, { onSuccess: () => onDeleted?.(id) })
        }}
      />

      <AddMemberDialog
        group={addMemberTarget}
        candidates={addMemberCandidates}
        onOpenChange={(isOpen) => !isOpen && setAddMemberTarget(null)}
      />

      <MoveToGroupDialog
        target={moveTarget}
        candidates={moveCandidates}
        onOpenChange={(isOpen) => !isOpen && setMoveTarget(null)}
      />

      <RemoveMemberDialog
        target={removeMemberTarget}
        onOpenChange={(isOpen) => !isOpen && setRemoveMemberTarget(null)}
        isPending={removeMember.isPending}
        onConfirm={() => {
          if (!removeMemberTarget) return
          removeMember.mutate({
            groupId: removeMemberTarget.groupId,
            memberId: removeMemberTarget.member.principalId,
          })
        }}
      />
    </>
  )
}
