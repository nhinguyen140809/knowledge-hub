import { Card, Skeleton } from '@heroui/react'
import { type ReactNode } from 'react'

interface StatCardProps {
  label: string
  value: ReactNode
  icon?: ReactNode
  isLoading?: boolean
}

/** A single dashboard metric tile: big value over a muted label, optional icon
 *  badge. Colors come from HeroUI semantic tokens so it flips with the theme. */
export function StatCard({ label, value, icon, isLoading = false }: StatCardProps) {
  return (
    <Card>
      {/* Card.Content stacks its children by default; force a row so the icon
          sits beside the value rather than above it. */}
      <Card.Content className="flex flex-row items-center gap-4">
        {icon && (
          <div className="bg-accent/10 text-accent flex size-11 shrink-0 items-center justify-center rounded-xl">
            {icon}
          </div>
        )}
        <div className="flex flex-col gap-1">
          {isLoading ? (
            <Skeleton className="h-7 w-10 rounded" />
          ) : (
            <span className="text-foreground text-2xl font-semibold">{value}</span>
          )}
          <span className="text-muted text-sm">{label}</span>
        </div>
      </Card.Content>
    </Card>
  )
}
