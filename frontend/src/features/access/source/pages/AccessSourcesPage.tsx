import { ScrollShadow, Skeleton, Tabs } from '@heroui/react'
import { lazy, Suspense } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSetHeaderActions } from '@/lib/store/header.store'
import { DefaultPolicyToggle } from '../components/DefaultPolicyToggle'
import { SourceListPanel } from '../components/SourceListPanel'
import { SourcePrincipalsPanel } from '../components/SourcePrincipalsPanel'
import { useSourceAccessSelection, type SourceAccessView } from '../hooks/useSourceAccessSelection'

// React Flow and its layout engine are the heaviest thing the app pulls in, and
// this tab is closed by default — load them the first time it is opened.
const SourceAccessGraph = lazy(() =>
  import('../components/SourceAccessGraph').then((m) => ({ default: m.SourceAccessGraph })),
)

/**
 * Source-centric access control: the mirror of {@link AccessPage}. The list on
 * the left selects a source; the right side answers "who can read it"
 * (Principals, with DIRECT grants revocable) and "why" (Graph). Interaction
 * state lives in {@link useSourceAccessSelection}.
 */
export function AccessSourcesPage() {
  const { selectedId, view, setView, toggleSelect } = useSourceAccessSelection()
  const navigate = useNavigate()

  // The default-policy control is page chrome, not page content — same reason
  // AccessPage puts it in the header rather than repeating it in the body.
  useSetHeaderActions(<DefaultPolicyToggle />)

  return (
    <div className="grid gap-4 lg:h-full lg:min-h-0 lg:grid-cols-[minmax(260px,1fr)_2fr]">
      <SourceListPanel selectedId={selectedId} onSelect={toggleSelect} />

      <Tabs
        selectedKey={view}
        onSelectionChange={(key) => setView(key as SourceAccessView)}
        className="lg:flex lg:min-h-0 lg:flex-col"
      >
        <Tabs.ListContainer>
          <Tabs.List aria-label="Source access views">
            <Tabs.Tab id="principals">
              Principals
              <Tabs.Indicator />
            </Tabs.Tab>
            <Tabs.Tab id="graph">
              Graph
              <Tabs.Indicator />
            </Tabs.Tab>
          </Tabs.List>
        </Tabs.ListContainer>

        <Tabs.Panel id="principals" className="pt-4 lg:flex lg:min-h-0 lg:flex-1 lg:flex-col">
          <ScrollShadow className="lg:min-h-0 lg:flex-1" offset={2}>
            <SourcePrincipalsPanel
              sourceId={selectedId}
              onSelectPrincipal={(principalId) =>
                navigate('/access', { state: { selectPrincipal: principalId } })
              }
            />
          </ScrollShadow>
        </Tabs.Panel>

        <Tabs.Panel id="graph" className="pt-4 lg:min-h-0 lg:flex-1">
          <Suspense fallback={<Skeleton className="h-105 w-full rounded-xl lg:h-full" />}>
            <SourceAccessGraph key={selectedId ?? 'none'} sourceId={selectedId} />
          </Suspense>
        </Tabs.Panel>
      </Tabs>
    </div>
  )
}
