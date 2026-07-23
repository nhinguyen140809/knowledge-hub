import { Chip, Popover, Skeleton, Switch } from '@heroui/react'
import { ShieldCheck, ShieldAlert, TriangleAlert, type LucideIcon } from 'lucide-react'
import { useDefaultPolicy, useSetDefaultPolicy } from '../hooks/useDefaultPolicy'
import { ALLOW, DENY, type DefaultPolicy } from '../types/access.type'

interface PolicyLook {
  color: 'default' | 'accent' | 'warning' | 'danger' | 'success'
  label: string
  icon: LucideIcon
}

// How each policy value presents in the header chip. DENY is the safe resting
// state (nothing is readable without a grant); ALLOW is the permissive one
// that opens every ungranted source, so it should read as the louder of the
// two — the chip is the only always-visible cue for a system-wide setting.
// TODO(human): fill both entries.
const POLICY_LOOK: Record<DefaultPolicy, PolicyLook> = {
  DENY: { color: 'success', label: 'Deny', icon: ShieldCheck },
  ALLOW: { color: 'danger', label: 'Allow', icon: ShieldAlert },
}

/**
 * System-wide fallback when no grant matches, surfaced in the app header. The
 * chip shows the current stance at a glance; opening it reveals what the
 * setting means and the switch to flip it. Flipping changes every principal's
 * effective permissions at once, so the switch reflects the in-flight value
 * while the mutation is pending rather than flickering back.
 */
export function DefaultPolicyToggle() {
  const policy = useDefaultPolicy()
  const setPolicy = useSetDefaultPolicy()

  if (policy.isPending) return <Skeleton className="h-7 w-24 rounded-full" />

  if (policy.isError) {
    return (
      <Chip size="lg" variant="soft" color="danger">
        <TriangleAlert size={16} />
        {(policy.error as Error).message}
      </Chip>
    )
  }

  if (!policy.data)
    return (
      <Chip size="lg" variant="soft" color="danger">
        <TriangleAlert size={16} />
        Policy unavailable
      </Chip>
    )

  // Optimistic only while in flight: `variables` outlives the mutation (it
  // sticks around after settling), so falling back to it unconditionally would
  // keep showing a value that failed to apply.
  const current = setPolicy.isPending ? setPolicy.variables! : policy.data
  const isAllow = current === ALLOW
  const look = POLICY_LOOK[current]

  return (
    <Popover>
      <Popover.Trigger aria-label="Default policy">
        <Chip size="lg" variant="soft" color={look.color} className="cursor-pointer">
          <look.icon size={16} />
          Policy: {look.label}
        </Chip>
      </Popover.Trigger>
      <Popover.Content className="max-w-72">
        <Popover.Dialog className="p-6">
          <Popover.Arrow />
          <Popover.Heading>Default policy</Popover.Heading>
          <p className="text-muted mt-2 text-sm">
            When no grant matches, a source is{' '}
            {isAllow ? 'readable by everyone' : 'hidden from everyone'}. This applies to every
            principal at once.
          </p>
          <Switch
            className="mt-4"
            isSelected={isAllow}
            isDisabled={setPolicy.isPending}
            onChange={(selected) => setPolicy.mutate(selected ? ALLOW : DENY)}
          >
            <Switch.Content>
              <Switch.Control>
                <Switch.Thumb />
              </Switch.Control>
              Allow by default
            </Switch.Content>
          </Switch>
        </Popover.Dialog>
      </Popover.Content>
    </Popover>
  )
}
