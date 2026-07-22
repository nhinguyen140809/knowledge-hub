import { Card, Chip } from '@heroui/react'
import { AlertTriangle } from 'lucide-react'
import type { ReactNode } from 'react'

interface ErrorStateProps {
  /** @default AlertTriangle — the icon rarely needs to vary; "something
   *  went wrong" doesn't have the per-context meaning "nothing here yet" does. */
  icon?: ReactNode
  description: string
  children?: ReactNode
}

/** Same anatomy as EmptyState (icon, description, optional action) but toned
 *  for "something went wrong" rather than "nothing here yet" — the two are
 *  different situations for the user and shouldn't share a color. */
export function ErrorState({
  icon = <AlertTriangle size={28} />,
  description,
  children,
}: ErrorStateProps) {
  return (
    <Card variant="transparent" className="border-danger/30 border border-dashed">
      <Card.Content className="flex flex-col items-center gap-3 py-10 text-center">
        <Chip variant="soft" color="danger" className="p-3">
          {icon}
        </Chip>
        <p className="text-danger text-sm">{description}</p>
        {children}
      </Card.Content>
    </Card>
  )
}
