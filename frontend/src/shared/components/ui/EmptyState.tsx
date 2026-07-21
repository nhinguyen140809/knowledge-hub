import { Card, Chip } from '@heroui/react'
import type { ReactNode } from 'react'

interface EmptyStateProps {
  icon: ReactNode
  description: string
  children?: ReactNode
}

/** Dashed-border placeholder for an empty list or unset selection: icon,
 *  description and an optional action (e.g. a button to create the first item). */
export function EmptyState({ icon, description, children }: EmptyStateProps) {
  return (
    <Card variant="transparent" className="border border-dashed">
      <Card.Content className="flex flex-col items-center gap-3 py-10 text-center">
        <Chip variant="soft" color="accent" className="p-3">
          {icon}
        </Chip>
        <p className="text-muted text-sm">{description}</p>
        {children}
      </Card.Content>
    </Card>
  )
}
