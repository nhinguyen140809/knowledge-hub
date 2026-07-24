import { Skeleton } from '@heroui/react'
import { Users } from 'lucide-react'
import { EmptyState } from '@/shared/components/ui/EmptyState'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { Tree } from '@/shared/components/ui/Tree'
import { AddMemberDialog } from './AddMemberDialog'
import { AddToGroupDialog } from './AddToGroupDialog'
import { DeletePrincipalDialog } from './DeletePrincipalDialog'
import { MoveToGroupDialog } from './MoveToGroupDialog'
import { PrincipalTreeContext } from './PrincipalTreeContext'
import { PrincipalTreeNode } from './PrincipalTreeNode'
import { RemoveMemberDialog } from './RemoveMemberDialog'
import { usePrincipalTree } from './usePrincipalTree'

interface PrincipalTreeProps {
  selectedId?: string | null
  onSelect?: (principalId: string) => void
  /** Fires after a principal is successfully deleted — the owner of the
   *  selection needs it to drop a selection that now points at nothing. */
  onDeleted?: (principalId: string) => void
}

/**
 * The principal hierarchy. Membership is a directed graph, not a tree: groups
 * nest, a principal can belong to several groups. All derivation lives in {@link usePrincipalTree}; this
 * component only maps each state to pixels.
 */
export function PrincipalTree({ selectedId, onSelect, onDeleted }: PrincipalTreeProps) {
  const tree = usePrincipalTree()

  if (tree.isPending) {
    return (
      <div className="flex flex-col gap-2">
        {Array.from({ length: 5 }).map((_, i) => (
          <Skeleton key={i} className="h-8 w-full rounded-lg" />
        ))}
      </div>
    )
  }

  if (tree.isError) return <ErrorState description={(tree.error as Error).message} />

  if (tree.isEmpty) {
    return <EmptyState icon={<Users size={28} />} description="No principals yet." />
  }

  return (
    <>
      <PrincipalTreeContext.Provider
        value={{
          byId: tree.byId,
          membership: tree.membership,
          adminCount: tree.adminCount,
          selectedId,
          onSelect,
          requestDelete: tree.setDeleteTarget,
          requestAddMember: tree.setAddMemberTarget,
          requestAddToGroup: tree.setAddToGroupTarget,
          requestMoveToGroup: tree.setMoveTarget,
          requestRemoveMember: tree.setRemoveMemberTarget,
        }}
      >
        <Tree>
          {tree.topLevel.map((p) => (
            <PrincipalTreeNode key={p.principalId} principal={p} path={[p.principalId]} />
          ))}
        </Tree>
      </PrincipalTreeContext.Provider>

      <DeletePrincipalDialog
        target={tree.deleteTarget}
        onOpenChange={(isOpen) => !isOpen && tree.setDeleteTarget(null)}
        onDeleted={onDeleted}
      />

      <AddMemberDialog
        group={tree.addMemberTarget}
        onOpenChange={(isOpen) => !isOpen && tree.setAddMemberTarget(null)}
      />

      <AddToGroupDialog
        target={tree.addToGroupTarget}
        candidates={tree.addToGroupCandidates}
        onOpenChange={(isOpen) => !isOpen && tree.setAddToGroupTarget(null)}
      />

      <MoveToGroupDialog
        target={tree.moveTarget}
        candidates={tree.moveCandidates}
        onOpenChange={(isOpen) => !isOpen && tree.setMoveTarget(null)}
      />

      <RemoveMemberDialog
        target={tree.removeMemberTarget}
        onOpenChange={(isOpen) => !isOpen && tree.setRemoveMemberTarget(null)}
      />
    </>
  )
}
