import { Surface, Chip, ScrollShadow } from '@heroui/react'
import { useState } from 'react'
import { ErrorState } from '@/shared/components/ui/ErrorState'
import { HitList } from '../components/HitList'
import { QueryForm } from '../components/QueryForm'
import { useSearch } from '../hooks/useSearch'
import type { QueryInput } from '../types/query.type'

/** Hybrid search over the knowledge graph. The submitted input is held in state
 *  rather than fired as a mutation, so re-running the same search is served from
 *  cache and the form stays editable without re-querying on every keystroke. */
export function QueryPage() {
  const [submitted, setSubmitted] = useState<QueryInput | null>(null)
  const { data, isPending, isError, error } = useSearch(submitted)

  return (
    <div className="flex h-full flex-col gap-6">
      <div className="flex shrink-0 flex-col gap-6">
        <Surface variant="transparent" className="mb-4">
          <QueryForm onSubmit={setSubmitted} />
        </Surface>

        {isError && <ErrorState description={(error as Error).message} />}

        {data?.servedFromCanonicalRef && (
          <Chip size="md" variant="soft" color="warning" className="self-start">
            requested ref not indexed, served from the canonical ref
          </Chip>
        )}
      </div>

      <ScrollShadow className="min-h-0 flex-1" offset={2}>
        <HitList hits={data?.hits} isPending={!!submitted && isPending} hasSearched={!!submitted} />
      </ScrollShadow>
    </div>
  )
}
