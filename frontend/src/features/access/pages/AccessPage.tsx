import { ScrollShadow, Skeleton, Tabs } from '@heroui/react'
import { lazy, Suspense } from 'react'
import { useSetHeaderActions } from '@/lib/store/header.store'
import { DefaultPolicyToggle } from '../components/DefaultPolicyToggle'
import { PrincipalCredentialsPanel } from '../components/PrincipalCredentialsPanel'
import { PrincipalSourcesPanel } from '../components/PrincipalSourcesPanel'
import { PrincipalsPanel } from '../components/PrincipalsPanel'
import { useAccessSelection, type AccessView } from '../hooks/useAccessSelection'

// React Flow and its layout engine are the heaviest thing the app pulls in, and
// this tab is closed by default — load them the first time it is opened.
const AccessGraph = lazy(() =>
  import('../components/AccessGraph').then((m) => ({ default: m.AccessGraph })),
)

/**
 * Principal-centric access control. The tree on the left is the selector — fast
 * to scan and keyboard navigable — while the right side answers two different
 * questions: "what does this principal have" (details) and "where does that
 * access come from" (graph). Interaction state lives in {@link useAccessSelection}.
 */
export function AccessPage() {
  const { selectedId, view, tracedSourceId, setView, toggleSelect, clearIfSelected, traceAccess } =
    useAccessSelection()

  // The default-policy control is page chrome, not page content — it lives in
  // the app header next to the title while this page is mounted.
  useSetHeaderActions(<DefaultPolicyToggle />)

  // From lg up the page fills the main area and each pane scrolls on its own
  // (tree, details, graph) — the graph takes all remaining height instead of
  // a fixed box. Below lg everything stacks and the page scrolls as one.
  return (
    <div className="grid gap-4 lg:h-full lg:min-h-0 lg:grid-cols-[minmax(260px,1fr)_2fr]">
      <PrincipalsPanel
        selectedId={selectedId}
        onSelect={toggleSelect}
        onDeleted={clearIfSelected}
      />

      <Tabs
        selectedKey={view}
        onSelectionChange={(key) => setView(key as AccessView)}
        className="lg:flex lg:min-h-0 lg:flex-col"
      >
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
              <PrincipalSourcesPanel principalId={selectedId} onTrace={traceAccess} />
              <PrincipalCredentialsPanel principalId={selectedId} />
            </div>
          </ScrollShadow>
        </Tabs.Panel>

        <Tabs.Panel id="graph" className="pt-4 lg:min-h-0 lg:flex-1">
          <Suspense fallback={<Skeleton className="h-105 w-full rounded-xl lg:h-full" />}>
            <AccessGraph
              selectedId={selectedId}
              onSelect={toggleSelect}
              traceSourceId={tracedSourceId}
            />
          </Suspense>
        </Tabs.Panel>
      </Tabs>
    </div>
  )
}
