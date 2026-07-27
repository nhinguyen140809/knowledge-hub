import { Button, Card, Input, TextField } from '@heroui/react'
import { Check, Pencil, Trash2, X } from 'lucide-react'
import { useState } from 'react'
import { type Connection, useConnectionStore } from '@/lib/store/connections.store'
import { ConfirmDialog } from '@/shared/components/ui/ConfirmDialog'
import { IconButton } from '@/shared/components/ui/IconButton'

function DeleteConnectionButton({ id, label }: { id: string; label: string }) {
  const removeConnection = useConnectionStore((s) => s.removeConnection)
  return (
    <ConfirmDialog
      trigger={
        <IconButton tooltip="Remove connection" size="sm" variant="danger-soft">
          <Trash2 size={16} />
        </IconButton>
      }
      icon={<Trash2 className="size-5" />}
      heading="Remove this connection?"
      message={
        <p>
          <strong>{label}</strong> will be forgotten. You can add it again later with its API key.
        </p>
      }
      confirmButton={
        <Button slot="close" variant="danger" onPress={() => removeConnection(id)}>
          Remove
        </Button>
      }
    />
  )
}

interface ConnectionRowProps {
  connection: Connection
  isActive: boolean
}

/** One row in the connections registry: label + baseUrl, with inline rename
 *  (label only — baseUrl changes go through /connect, which validates
 *  against the backend) and remove. */
export function ConnectionRow({ connection, isActive }: ConnectionRowProps) {
  const renameConnection = useConnectionStore((s) => s.renameConnection)
  const [isEditing, setEditing] = useState(false)
  const [label, setLabel] = useState(connection.label)
  const [error, setError] = useState<string | null>(null)

  function startEdit() {
    setLabel(connection.label)
    setError(null)
    setEditing(true)
  }

  function cancelEdit() {
    setEditing(false)
    setError(null)
  }

  function save() {
    const trimmed = label.trim()
    if (!trimmed) {
      setError('Label cannot be empty')
      return
    }
    renameConnection(connection.id, trimmed)
    setEditing(false)
  }

  return (
    <Card className="flex-row items-center justify-between gap-12 px-6 py-4">
      <div className="flex min-w-0 flex-1 flex-col gap-2">
        {isEditing ? (
          <TextField
            value={label}
            onChange={(value) => {
              setLabel(value)
              setError(null)
            }}
            isInvalid={!!error}
          >
            <Input
              autoFocus
              onKeyDown={(e) => {
                if (e.key === 'Enter') save()
                if (e.key === 'Escape') cancelEdit()
              }}
              className="focus:border-accent w-full rounded-none border-0 border-b border-transparent bg-transparent px-0 py-1 text-sm font-medium shadow-none focus:bg-transparent focus:shadow-none focus:ring-0"
            />
            {error && <p className="text-danger text-xs">{error}</p>}
          </TextField>
        ) : (
          <span className="truncate text-sm font-medium">
            {connection.label}
            {isActive && <span className="text-success ml-2 text-xs font-bold">Active</span>}
          </span>
        )}
        <span className="text-muted truncate font-mono text-sm">{connection.baseUrl}</span>
      </div>

      <div className="flex shrink-0 items-center gap-1">
        {isEditing ? (
          <>
            <IconButton tooltip="Save" size="sm" onPress={save}>
              <Check size={16} />
            </IconButton>
            <IconButton tooltip="Cancel" size="sm" variant="ghost" onPress={cancelEdit}>
              <X size={16} />
            </IconButton>
          </>
        ) : (
          <>
            <IconButton tooltip="Rename" size="sm" variant="ghost" onPress={startEdit}>
              <Pencil size={16} />
            </IconButton>
            <DeleteConnectionButton id={connection.id} label={connection.label} />
          </>
        )}
      </div>
    </Card>
  )
}
