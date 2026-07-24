import { Chip } from '@heroui/react'
import { FolderClosed, RotateCcw, User, type LucideIcon } from 'lucide-react'
import { Tree } from '@/shared/components/ui/Tree'
import { copyText } from '@/shared/hooks/useCopyToClipboard'
import { canDelete, canHaveMembers, canJoinGroup } from '../../lib/principal.rules'
import { PrincipalContextMenu } from './PrincipalContextMenu'
import { usePrincipalTreeContext } from './PrincipalTreeContext'
import type { Principal, PrincipalType } from '../../types/access.type'

/** Row icon per principal type — shape *and* color, because two same-size
 *  person glyphs in the same tone read as identical at 15px. Groups take the
 *  accent color the graph also gives them, so both views speak the same
 *  visual language. Swap an entry here to restyle every row at once. */
const PRINCIPAL_TYPE_CONFIG: Record<PrincipalType, { icon: LucideIcon; className: string }> = {
  GROUP: { icon: FolderClosed, className: 'text-accent' },
  SUBJECT: { icon: User, className: 'text-muted' },
}

const icon = (principal: Principal) => {
  const config = PRINCIPAL_TYPE_CONFIG[principal.type]
  return <config.icon size={15} className={config.className} />
}

const roleChip = (principal: Principal) =>
  principal.role === 'ADMIN' ? (
    <Chip size="sm" variant="soft" color="accent">
      admin
    </Chip>
  ) : null

interface PrincipalTreeNodeProps {
  principal: Principal
  path: string[]
  /** The group this row is nested under in *this* render — undefined at the
   *  top level, where there's nothing to remove membership from. */
  parentGroupId?: string
}

/** One principal row, recursing into its members. Keyed by path rather than
 *  id — membership is a graph, not a tree, so the same principal may
 *  legitimately render under several parents — and recursion stops when a
 *  principal repeats on the current path so a cycle can't render forever. */
export function PrincipalTreeNode({ principal, path, parentGroupId }: PrincipalTreeNodeProps) {
  const {
    byId,
    membership,
    adminCount,
    selectedId,
    onSelect,
    requestDelete,
    requestAddMember,
    requestAddToGroup,
    requestMoveToGroup,
    requestRemoveMember,
  } = usePrincipalTreeContext()

  const key = path.join('/')
  const childIds = membership[principal.principalId] ?? []
  // Each menu action is gated by its own rule. Copy id always applies, so the
  // menu always renders — even the last admin, whose every mutating action is
  // disallowed, can still copy its id.
  const contextMenu = (
    <PrincipalContextMenu
      onCopyId={() => void copyText(principal.principalId)}

      onDelete={canDelete(principal, adminCount) ? () => requestDelete(principal) : undefined}

      onAddMember={canHaveMembers(principal) ? () => requestAddMember(principal) : undefined}

      onAddToGroup={canJoinGroup(principal) ? () => requestAddToGroup(principal) : undefined}

      onMoveToGroup={
        canJoinGroup(principal)
          ? () => requestMoveToGroup({ principal, fromGroupId: parentGroupId })
          : undefined
      }

      onRemoveMember={
        parentGroupId
          ? () => requestRemoveMember({ groupId: parentGroupId, member: principal })
          : undefined
      }
    />
  )
  const row = {
    label: principal.principalId,
    icon: icon(principal),
    trailing: roleChip(principal),
    isSelected: selectedId === principal.principalId,
    onSelect: () => onSelect?.(principal.principalId),
    contextMenu,
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
            parentGroupId={principal.principalId}
          />
        )
      })}
    </Tree.Group>
  )
}
