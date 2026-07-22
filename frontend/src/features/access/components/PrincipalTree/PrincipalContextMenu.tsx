import { Dropdown, Label } from '@heroui/react'
import { FolderInput, Trash2, UserMinus, UserPlus } from 'lucide-react'

interface PrincipalContextMenuProps {
  onDelete: () => void
  onAddMember?: () => void
  onMoveToGroup: () => void
  onRemoveMember?: () => void
}

/** Add member only makes sense on a group — adding to a subject would fail the
 *  backend's `requireGroup` check. Remove from group only makes sense where
 *  this row actually has a parent in the current render. Move applies to any
 *  principal (subjects and groups can both be members). All are omitted
 *  rather than shown and left to fail. */
export function PrincipalContextMenu({
  onDelete,
  onAddMember,
  onMoveToGroup,
  onRemoveMember,
}: PrincipalContextMenuProps) {
  return (
    <Dropdown.Menu
      onAction={(key) => {
        if (key === 'delete') onDelete()
        if (key === 'add-member') onAddMember?.()
        if (key === 'move-to-group') onMoveToGroup()
        if (key === 'remove-member') onRemoveMember?.()
      }}
    >
      {onAddMember && (
        <Dropdown.Item id="add-member" textValue="Add member">
          <UserPlus size={14} />
          <Label>Add member</Label>
        </Dropdown.Item>
      )}
      <Dropdown.Item id="move-to-group" textValue="Move to group">
        <FolderInput size={14} />
        <Label>Move to group</Label>
      </Dropdown.Item>
      {onRemoveMember && (
        <Dropdown.Item id="remove-member" textValue="Remove from group">
          <UserMinus size={14} />
          <Label>Remove from group</Label>
        </Dropdown.Item>
      )}
      <Dropdown.Item id="delete" textValue="Delete" variant="danger">
        <Trash2 size={14} />
        <Label>Delete</Label>
      </Dropdown.Item>
    </Dropdown.Menu>
  )
}
