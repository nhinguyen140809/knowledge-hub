import type { Source } from '@/features/sources'
import type { SystemInfo } from '@/shared/types/system.type'
import type { DependencyStatus } from '../types/dashboard.type'
import { NON_PROD_PROFILES } from './health.util'

/** Maps 1:1 to HeroUI's semantic colors for the row's icon. */
export type AttentionTone = 'warning' | 'danger'

/** One thing worth the operator's notice on the dashboard. `id` keys the list
 *  and is stable per rule; `message` is the whole sentence shown. */
export interface AttentionItem {
  id: string
  tone: AttentionTone
  message: string
}

/** Everything the rules look at. All optional: each query resolves
 *  independently, and a rule simply doesn't fire while its input is missing. */
export interface AttentionInputs {
  dependencies?: DependencyStatus[]
  systemInfo?: SystemInfo
  sources?: Source[]
}

/** A single product-policy condition: returns the item to show, or null when
 *  it doesn't apply (input missing, or nothing wrong). */
type AttentionRule = (inputs: AttentionInputs) => AttentionItem | null

const pluralize = (n: number, one: string, many: string) => (n === 1 ? one : many)

/** A dependency that isn't answering is the most urgent thing here — retrieval
 *  and indexing both fail without it — so it leads and is the only danger. */
const dependencyDown: AttentionRule = ({ dependencies }) => {
  if (!dependencies) return null
  const down = dependencies.filter((d) => d.status === 'DOWN')
  if (down.length === 0) return null
  const names = down.map((d) => d.name).join(', ')
  return {
    id: 'deps-down',
    tone: 'danger',
    message:
      down.length === 1
        ? `${names} service is unreachable`
        : `${down.length} services are unreachable (${names})`,
  }
}

/** Sources that exist but were never indexed contribute nothing to retrieval —
 *  they look configured but are dead weight until synced. */
const sourcesNeverSynced: AttentionRule = ({ sources }) => {
  if (!sources) return null
  const count = sources.filter((s) => s.updatedAt === null).length
  if (count === 0) return null
  return {
    id: 'sources-unsynced',
    tone: 'warning',
    message: `${count} ${pluralize(count, 'source has', 'sources have')} never been synced`,
  }
}

/** A non-production profile on the connected backend is worth a heads-up — it
 *  usually means you're pointed at a scratch environment, not the real one. */
const nonProductionProfile: AttentionRule = ({ systemInfo }) => {
  if (!systemInfo) return null
  const flagged = systemInfo.activeProfiles.filter((p) =>
    NON_PROD_PROFILES.includes(p.toLowerCase()),
  )
  if (flagged.length === 0) return null
  return {
    id: 'non-prod',
    tone: 'warning',
    message: `Running in a non-production profile (${flagged.join(', ')})`,
  }
}

/** The whole rule set, most actionable first — the order the panel renders in.
 *  Add a condition by adding a rule here; nothing else changes. */
export const ATTENTION_RULES: AttentionRule[] = [
  dependencyDown,
  sourcesNeverSynced,
  nonProductionProfile,
]
