import { Switch } from '@heroui/react'
import { useDefaultPolicy, useSetDefaultPolicy } from '../hooks/useDefaultPolicy'
import { ALLOW, DENY } from '../types/access.type'

/**
 * System-wide fallback when no grant matches. Selected = ALLOW, unselected =
 * DENY — flipping this changes every principal's effective permissions at
 * once, so the switch reflects the in-flight value while the mutation is
 * pending rather than flickering back to the old state.
 */
export function DefaultPolicyToggle() {
  const policy = useDefaultPolicy()
  const setPolicy = useSetDefaultPolicy()

  if (!policy.data) return null

  const isSelected = (setPolicy.variables ?? policy.data) === ALLOW

  return (
    <Switch
      isSelected={isSelected}
      isDisabled={setPolicy.isPending}
      onChange={(selected) => setPolicy.mutate(selected ? ALLOW : DENY)}
    >
      <Switch.Content>
        <Switch.Control>
          <Switch.Thumb />
        </Switch.Control>
        {isSelected ? 'Allow' : 'Deny'}
      </Switch.Content>
    </Switch>
  )
}
