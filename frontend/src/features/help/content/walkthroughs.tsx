import type { ReactNode } from 'react'
import { AccessInheritanceFigure, MembershipFigure } from '../components/ConceptFigure'
import { Note, Steps } from '../components/GuideBlocks'

export interface Walkthrough {
  id: string
  title: string
  body: ReactNode
}

// The guide content: one entry per task an admin actually gets stuck on. Kept
// task-first (do this, then that), with the concept explained only where the
// task needs it.
export const WALKTHROUGHS: Walkthrough[] = [
  {
    id: 'dashboard',
    title: 'Reading the dashboard',
    body: (
      <>
        <Steps>
          <li>
            <strong>Services</strong> shows each backend dependency. Anything not healthy is where
            to look first when something misbehaves.
          </li>
          <li>
            <strong>Retrieval</strong> summarises index size and search activity, a quick sense of
            how much has been ingested.
          </li>
          <li>
            <strong>Attention</strong> surfaces things that need a person: a core dependency down,
            sources never synced, or a non-production profile.
          </li>
        </Steps>
        <Note>
          Start here after connecting to confirm the instance is healthy before making changes.
        </Note>
      </>
    ),
  },
  {
    id: 'connection',
    title: 'Connecting and switching backend',
    body: (
      <>
        <Steps>
          <li>The switcher in the sidebar header shows the active backend and its URL.</li>
          <li>Open it to add a connection, switch between saved ones, or disconnect.</li>
          <li>
            A <strong>MOCK</strong> chip means the app is running against built-in sample data, with
            no real backend behind it.
          </li>
        </Steps>
        <Note>Disconnecting returns you to the connect screen; your saved connections stay.</Note>
      </>
    ),
  },
  {
    id: 'membership',
    title: 'Managing membership',
    body: (
      <>
        <Note>
          Membership is a graph, not a tree: a principal can belong to several groups at once.
        </Note>
        <Steps>
          <li>
            <strong>Add member</strong> (right-click a group) creates a brand new principal inside
            that group.
          </li>
          <li>
            <strong>Add to group</strong> gives an existing principal an extra membership, keeping
            the ones it already has.
          </li>
          <li>
            <strong>Move to group</strong> swaps one membership for another.
          </li>
          <li>
            <strong>Remove from group</strong> drops a single membership.
          </li>
        </Steps>
        <MembershipFigure />
      </>
    ),
  },
  {
    id: 'access',
    title: 'Giving and tracing access',
    body: (
      <>
        <Steps>
          <li>Pick a principal in the tree on the left.</li>
          <li>In the Sources panel, use Grant to give it read access to a source.</li>
          <li>
            Each source is tagged <strong>direct</strong>, <strong>inherited</strong>,{' '}
            <strong>admin</strong>, or <strong>policy</strong>, showing where the access comes from.
          </li>
          <li>Click a source to open the Graph tab with its access path highlighted.</li>
          <li>Only a direct grant can be revoked from this panel.</li>
        </Steps>
        <AccessInheritanceFigure />
      </>
    ),
  },
  {
    id: 'sources',
    title: 'Adding and syncing a source',
    body: (
      <Steps>
        <li>On Sources, choose Add source and pick Git or filesystem.</li>
        <li>
          For Git, set the ref; for either, set the include and ignore globs to scope what gets
          indexed.
        </li>
        <li>
          Open a source and run Sync. The result reports indexed, reindexed, evicted, and skipped
          counts.
        </li>
        <li>
          Sync is incremental and idempotent, so running it again is safe and usually a no-op.
        </li>
      </Steps>
    ),
  },
  {
    id: 'query',
    title: 'Searching the knowledge graph',
    body: (
      <>
        <Steps>
          <li>
            Type your question in the Query box and press Search. An empty query does nothing.
          </li>
          <li>
            Narrow with <strong>Source</strong> to one source and <strong>Ref</strong> to one
            version (defaults to <strong>canonical</strong>).
          </li>
          <li>
            Use <strong>Type</strong> to keep only <strong>code</strong>, <strong>doc</strong>,{' '}
            <strong>requirement</strong>, or <strong>commit</strong> hits, and{' '}
            <strong>Top K</strong> to cap how many come back (blank uses the server default).
          </li>
          <li>
            If the ref you asked for is not indexed, a chip says so and results come from the
            canonical ref instead. That is expected, not an error.
          </li>
        </Steps>
        <Note>
          Every filter only narrows the search inside what you may already read. None of them can
          widen access to sources you were not granted.
        </Note>
      </>
    ),
  },
  {
    id: 'credentials',
    title: 'Issuing and revoking credentials',
    body: (
      <Steps>
        <li>Select a principal and open its Credentials panel.</li>
        <li>
          Issue a credential, then copy the secret right away. It is shown once and cannot be
          retrieved again.
        </li>
        <li>
          Revoke breaks the key immediately and cannot be undone. Issue a new one if a key is lost.
        </li>
      </Steps>
    ),
  },
  {
    id: 'policy',
    title: 'The default policy',
    body: (
      <>
        <Steps>
          <li>The policy chip in the header shows the current stance, Deny or Allow.</li>
          <li>Open it to flip the system wide fallback that applies when no grant matches.</li>
        </Steps>
        <Note>Deny keeps sources private until granted; Allow opens every ungranted source.</Note>
      </>
    ),
  },
  {
    id: 'tips',
    title: 'Tips and shortcuts',
    body: (
      <Steps>
        <li>Press Cmd or Ctrl + K anywhere to open the command palette.</li>
        <li>Type to jump to a principal or source, then Enter to open it.</li>
        <li>Inside the palette, the arrow keys move the highlight and Esc closes it.</li>
      </Steps>
    ),
  },
]
