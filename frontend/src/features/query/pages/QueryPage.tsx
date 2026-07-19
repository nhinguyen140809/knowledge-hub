import { Card, Chip } from '@heroui/react'
import { useState } from 'react'
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
    <div className="flex flex-col gap-6">
      <Card>
        <Card.Content>
          <QueryForm onSubmit={setSubmitted} />
        </Card.Content>
      </Card>

      {isError && <p className="text-danger text-sm">{(error as Error).message}</p>}

      {data?.servedFromCanonicalRef && (
        <Chip size="sm" variant="soft" color="warning" className="self-start">
          requested ref not indexed — served from the canonical ref
        </Chip>
      )}

      <HitList hits={data?.hits} isPending={!!submitted && isPending} hasSearched={!!submitted} />
    </div>
  )
}
