import { Chip } from '@heroui/react'
import type { GrantOrigin } from '../types/access.type'

/** Each source/principal arrives already tagged with its origin; this only
 *  maps the tag to a look. Shared between the principal-centric and
 *  source-centric access panels. */
const GRANT_ORIGIN_CONFIG: Record<
  GrantOrigin,
  { color: 'default' | 'accent' | 'warning' | 'danger'; label: string }
> = {
  DIRECT: { color: 'default', label: 'direct' },
  INHERITED: { color: 'accent', label: 'inherited' },
  ADMIN: { color: 'danger', label: 'admin' },
  POLICY: { color: 'warning', label: 'policy' },
}

export function OriginChip({ origin }: { origin: GrantOrigin }) {
  const config = GRANT_ORIGIN_CONFIG[origin]
  return (
    <Chip size="sm" variant="soft" color={config.color}>
      {config.label}
    </Chip>
  )
}
