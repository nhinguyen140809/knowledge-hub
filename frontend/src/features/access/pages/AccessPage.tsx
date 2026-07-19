import { Card } from '@heroui/react'
import { useState } from 'react'
import { PrincipalCredentialsPanel } from '../components/PrincipalCredentialsPanel'
import { PrincipalSourcesPanel } from '../components/PrincipalSourcesPanel'
import { PrincipalTree } from '../components/PrincipalTree'

/**
 * Principal-centric access control: pick a principal on the left, inspect what
 * it can read and which credentials it holds on the right.
 */
export function AccessPage() {
  const [selectedId, setSelectedId] = useState<string | null>(null)

  return (
    <div className="grid gap-4 lg:grid-cols-[minmax(260px,1fr)_2fr]">
      <Card className="lg:sticky lg:top-0">
        <Card.Header>
          <Card.Title>Principals</Card.Title>
          <Card.Description>Groups and subjects</Card.Description>
        </Card.Header>
        <Card.Content>
          <PrincipalTree selectedId={selectedId} onSelect={setSelectedId} />
        </Card.Content>
      </Card>

      <div className="flex flex-col gap-4">
        <PrincipalSourcesPanel principalId={selectedId} />
        <PrincipalCredentialsPanel principalId={selectedId} />
      </div>
    </div>
  )
}
