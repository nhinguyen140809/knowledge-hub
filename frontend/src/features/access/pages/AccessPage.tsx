import { Card, ScrollShadow, Skeleton, Tabs } from '@heroui/react'
import { lazy, Suspense, useState } from 'react'
import { useSetHeaderActions } from '@/lib/store/header.store'
import { SUMMARY_SEP } from '@/shared/constants'
import { AddPrincipalDialog } from '../components/AddPrincipalDialog'
import { DefaultPolicyToggle } from '../components/DefaultPolicyToggle'
import { PrincipalCredentialsPanel } from '../components/PrincipalCredentialsPanel'
import { PrincipalSourcesPanel } from '../components/PrincipalSourcesPanel'
import { PrincipalTree } from '../components/PrincipalTree'
import { usePrincipalGraph } from '../hooks/usePrincipals'
import { summarizePrincipals } from '../lib/principal.rules'

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

  // Clicking the already-selected principal deselects it — the only gesture
  // that brings the graph back to the org-wide overview without adding a
  // dedicated "clear" control. Works from the tree and the graph alike.
  const toggleSelect = (id: string) => setSelectedId((prev) => (prev === id ? null : id))

  // The default-policy control is page chrome, not page content — it lives in
  // the app header next to the title while this page is mounted.
  useSetHeaderActions(<DefaultPolicyToggle />)

  // Cached — the tree renders from this same query, so reading it here is free.
  // A live breakdown replaces the static "Groups and subjects" caption once
  // data lands; the counts (groups, admins) aren't obvious from the tree alone.
  const graph = usePrincipalGraph()
  const summary = graph.data ? summarizePrincipals(graph.data.principals) : null
  const principalsCaption = summary
    ? [`${summary.total} principals`, `${summary.groups} groups`, `${summary.admins} admins`].join(
        SUMMARY_SEP,
      )
    : 'Groups and subjects'

  // From lg up the page fills the main area and each pane scrolls on its own
  // (tree, details, graph) — the graph takes all remaining height instead of
  // a fixed box. Below lg everything stacks and the page scrolls as one.
  return (
    <div className="grid gap-4 lg:h-full lg:min-h-0 lg:grid-cols-[minmax(260px,1fr)_2fr]">
      <Card
        variant="transparent"
        className="self-start lg:flex lg:min-h-0 lg:flex-col lg:self-stretch"
      >
        <Card.Header className="flex-row items-start justify-between">
          <div>
            <Card.Title className="text-accent text-lg font-bold">Principals</Card.Title>
            <Card.Description>{principalsCaption}</Card.Description>
          </div>
          <AddPrincipalDialog />
        </Card.Header>
        <Card.Content className="lg:flex lg:min-h-0 lg:flex-1 lg:flex-col">
          {/* offset absorbs the subpixel gap between scrollTop and
                scrollHeight on scaled displays, otherwise the bottom shadow
                never clears at the end. */}
          <ScrollShadow className="lg:min-h-0 lg:flex-1" offset={2}>
            <PrincipalTree
              selectedId={selectedId}
              onSelect={toggleSelect}
              onDeleted={(id) => id === selectedId && setSelectedId(null)}
            />
          </ScrollShadow>
        </Card.Content>
      </Card>

      <Tabs defaultSelectedKey="details" className="lg:flex lg:min-h-0 lg:flex-col">
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

        <Tabs.Panel id="details" className="pt-4 lg:flex lg:min-h-0 lg:flex-1 lg:flex-col">
          <ScrollShadow className="lg:min-h-0 lg:flex-1" offset={2}>
            <div className="flex flex-col gap-4">
              <PrincipalSourcesPanel principalId={selectedId} />
              <PrincipalCredentialsPanel principalId={selectedId} />
            </div>
          </ScrollShadow>
        </Tabs.Panel>

        <Tabs.Panel id="graph" className="pt-4 lg:min-h-0 lg:flex-1">
          <Suspense fallback={<Skeleton className="h-105 w-full rounded-xl lg:h-full" />}>
            <AccessGraph selectedId={selectedId} onSelect={toggleSelect} />
          </Suspense>
        </Tabs.Panel>
      </Tabs>
    </div>
  )
}
