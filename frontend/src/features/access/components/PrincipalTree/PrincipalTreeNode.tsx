import { Chip } from '@heroui/react'
import { RotateCcw, User, Users } from 'lucide-react'
import { Tree } from '@/shared/components/ui/Tree'
import { PrincipalContextMenu } from './PrincipalContextMenu'
import type { MoveToGroupTarget } from './MoveToGroupDialog'
import type { RemoveMemberTarget } from './RemoveMemberDialog'
import type { Principal } from '../../types/access.type'

const icon = (principal: Principal) =>
  principal.type === 'GROUP' ? <Users size={15} /> : <User size={15} />

const roleChip = (principal: Principal) =>
  principal.role === 'ADMIN' ? (
    <Chip size="sm" variant="soft" color="accent">
      admin
    </Chip>
  ) : null

interface PrincipalTreeNodeProps {
  principal: Principal
  path: string[]
  byId: Map<string, Principal>
  membership: Record<string, string[]>
  /** The group this row is nested under in *this* render — undefined at the
   *  top level, where there's nothing to remove membership from. */
  parentGroupId?: string
  selectedId?: string | null
  onSelect?: (principalId: string) => void
  onDeleteRequest: (principal: Principal) => void
  onAddMemberRequest: (group: Principal) => void
  onMoveToGroupRequest: (target: MoveToGroupTarget) => void
  onRemoveMemberRequest: (target: RemoveMemberTarget) => void
}

/** One principal row, recursing into its members. Keyed by path rather than
 *  id — membership is a graph, not a tree, so the same principal may
 *  legitimately render under several parents — and recursion stops when a
 *  principal repeats on the current path so a cycle can't render forever. */
export function PrincipalTreeNode({
  principal,
  path,
  byId,
  membership,
  parentGroupId,
  selectedId,
  onSelect,
  onDeleteRequest,
  onAddMemberRequest,
  onMoveToGroupRequest,
  onRemoveMemberRequest,
}: PrincipalTreeNodeProps) {
  const key = path.join('/')
  const childIds = membership[principal.principalId] ?? []
  const row = {
    label: principal.principalId,
    icon: icon(principal),
    trailing: roleChip(principal),
    isSelected: selectedId === principal.principalId,
    onSelect: () => onSelect?.(principal.principalId),
    contextMenu: (
      <PrincipalContextMenu
        onDelete={() => onDeleteRequest(principal)}
        onAddMember={principal.type === 'GROUP' ? () => onAddMemberRequest(principal) : undefined}
        onMoveToGroup={() => onMoveToGroupRequest({ principal, fromGroupId: parentGroupId })}
        onRemoveMember={
          parentGroupId
            ? () => onRemoveMemberRequest({ groupId: parentGroupId, member: principal })
            : undefined
        }
      />
    ),
  }

  if (childIds.length === 0) return <Tree.Item key={key} {...row} />

  return (
    <Tree.Group key={key} defaultExpanded {...row}>
      {childIds.map((childId) => {
        const child = byId.get(childId)
        if (!child) return null
        if (path.includes(childId)) {
          return (
            <Tree.Item
              key={`${key}/${childId}`}
              label={childId}
              icon={icon(child)}
              trailing={
                <Chip size="sm" variant="soft" color="warning">
                  <RotateCcw size={12} />
                  cycle
                </Chip>
              }
            />
          )
        }
        return (
          <PrincipalTreeNode
            key={`${key}/${childId}`}
            principal={child}
            path={[...path, childId]}
            byId={byId}
            membership={membership}
            parentGroupId={principal.principalId}
            selectedId={selectedId}
            onSelect={onSelect}
            onDeleteRequest={onDeleteRequest}
            onAddMemberRequest={onAddMemberRequest}
            onMoveToGroupRequest={onMoveToGroupRequest}
            onRemoveMemberRequest={onRemoveMemberRequest}
          />
        )
      })}
    </Tree.Group>
  )
}
