import { Card, Skeleton, Tabs } from '@heroui/react'
import { lazy, Suspense, useState } from 'react'
import { AddPrincipalDialog } from '../components/AddPrincipalDialog'
import { DefaultPolicyToggle } from '../components/DefaultPolicyToggle'
import { PrincipalCredentialsPanel } from '../components/PrincipalCredentialsPanel'
import { PrincipalSourcesPanel } from '../components/PrincipalSourcesPanel'
import { PrincipalTree } from '../components/PrincipalTree'

// React Flow and its layout engine are the heaviest thing the app pulls in, and
// this tab is closed by default — load them the first time it is opened.
const AccessGraph = lazy(() =>
  import('../components/AccessGraph').then((m) => ({ default: m.AccessGraph })),
)

/**
 * Principal-centric access control. The tree on the left is the selector — fast
 * to scan and keyboard navigable — while the right side answers two different
 * questions: "what does this principal have" (details) and "where does that
 * access come from" (graph).
 */
export function AccessPage() {
  const [selectedId, setSelectedId] = useState<string | null>(null)

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-col items-start gap-4 border-b pb-4">
        <h1 className="text-foreground text-md font-bold">Default policy</h1>
        <DefaultPolicyToggle />
      </div>

      <div className="grid gap-4 lg:grid-cols-[minmax(260px,1fr)_2fr]">
        <Card className="self-start px-6">
          <Card.Header className="flex-row items-start justify-between">
            <div>
              <Card.Title className="text-accent text-lg font-bold">Principals</Card.Title>
              <Card.Description>Groups and subjects</Card.Description>
            </div>
            <AddPrincipalDialog />
          </Card.Header>
          <Card.Content>
            <PrincipalTree selectedId={selectedId} onSelect={setSelectedId} />
          </Card.Content>
        </Card>

        <Tabs defaultSelectedKey="details">
          <Tabs.ListContainer>
            <Tabs.List aria-label="Access views">
              <Tabs.Tab id="details">
                Details
                <Tabs.Indicator />
              </Tabs.Tab>
              <Tabs.Tab id="graph">
                Graph
                <Tabs.Indicator />
              </Tabs.Tab>
            </Tabs.List>
          </Tabs.ListContainer>

          <Tabs.Panel id="details" className="flex flex-col gap-4 pt-4">
            <PrincipalSourcesPanel principalId={selectedId} />
            <PrincipalCredentialsPanel principalId={selectedId} />
          </Tabs.Panel>

          <Tabs.Panel id="graph" className="pt-4">
            <Suspense fallback={<Skeleton className="h-[420px] w-full rounded-xl" />}>
              <AccessGraph selectedId={selectedId} onSelect={setSelectedId} />
            </Suspense>
          </Tabs.Panel>
        </Tabs>
      </div>
    </div>
  )
}
