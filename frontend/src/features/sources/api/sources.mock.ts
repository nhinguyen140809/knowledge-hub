import type { Source, SourceStatus, SyncResult } from '../types/source.type'

const MOCK_NOW = new Date('2026-07-21T10:00:00Z').getTime()

/** Staggered by list position so sorting by last-update has something to
 *  show. Index 4 is left never-synced to exercise that state too. */
function mockUpdatedAt(index: number): string | null {
  if (index === 4) return null
  return new Date(MOCK_NOW - index * 7 * 60 * 60 * 1000).toISOString()
}

/** Sample sources so the UI can be explored in mock mode without a backend. */
const mockSourceBases: Omit<Source, 'updatedAt'>[] = [
  {
    id: 'engineering-wiki',
    type: 'GIT',
    uriOrPath: 'https://github.com/acme/engineering-wiki.git',
    ref: 'main',
    include: ['**/*.md'],
    ignore: ['archive/**'],
    name: 'Engineering Wiki',
    description: 'Internal engineering handbook and runbooks.',
  },
  {
    id: 'product-docs',
    type: 'GIT',
    uriOrPath: 'https://github.com/acme/product-docs.git',
    ref: 'release',
    include: [],
    ignore: [],
    name: 'Product Docs',
    description: 'Public product documentation and API references.',
  },
  {
    id: 'support-macros',
    type: 'FS',
    uriOrPath: '/srv/knowledge/support',
    ref: null,
    include: ['**/*.txt', '**/*.md'],
    ignore: [],
    name: 'Support Macros',
    description: null,
  },
  {
    id: 'design-system',
    type: 'GIT',
    uriOrPath: 'https://github.com/acme/design-system.git',
    ref: 'main',
    include: ['**/*.mdx'],
    ignore: ['node_modules/**'],
    name: 'Design System',
    description: 'Component guidelines, tokens and usage docs.',
  },
  {
    id: 'sre-runbooks',
    type: 'GIT',
    uriOrPath: 'https://github.com/acme/sre-runbooks.git',
    ref: 'main',
    include: [],
    ignore: ['drafts/**'],
    name: 'SRE Runbooks',
    description: 'Incident response and on-call procedures.',
  },
  {
    id: 'legal-templates',
    type: 'FS',
    uriOrPath: '/srv/knowledge/legal',
    ref: null,
    include: ['**/*.docx', '**/*.pdf'],
    ignore: [],
    name: 'Legal Templates',
    description: 'Contract and NDA templates for the legal team.',
  },
  {
    id: 'onboarding-guide',
    type: 'GIT',
    uriOrPath: 'https://github.com/acme/onboarding.git',
    ref: 'main',
    include: ['**/*.md'],
    ignore: [],
    name: 'Onboarding Guide',
    description: 'New hire checklist and first-week resources.',
  },
  {
    id: 'infra-terraform',
    type: 'GIT',
    uriOrPath: 'https://github.com/acme/infra.git',
    ref: 'develop',
    include: ['**/*.tf', '**/*.md'],
    ignore: ['.terraform/**'],
    name: 'Infra (Terraform)',
    description: null,
  },
  {
    id: 'sales-playbook',
    type: 'FS',
    uriOrPath: '/srv/knowledge/sales',
    ref: null,
    include: [],
    ignore: ['archive/**'],
    name: 'Sales Playbook',
    description: 'Pitch decks, objection handling, pricing sheets.',
  },
  {
    id: 'api-reference',
    type: 'GIT',
    uriOrPath: 'https://github.com/acme/api-reference.git',
    ref: 'main',
    include: ['**/*.yaml', '**/*.md'],
    ignore: [],
    name: 'API Reference',
    description: 'OpenAPI specs and generated endpoint docs.',
  },
  {
    id: 'hr-policies',
    type: 'FS',
    uriOrPath: '/srv/knowledge/hr',
    ref: null,
    include: ['**/*.pdf'],
    ignore: [],
    name: 'HR Policies',
    description: null,
  },
  {
    id: 'data-pipelines',
    type: 'GIT',
    uriOrPath: 'https://github.com/acme/data-pipelines.git',
    ref: 'main',
    include: ['**/*.md', '**/*.sql'],
    ignore: ['tmp/**'],
    name: 'Data Pipelines',
    description: 'ETL job docs and schema references.',
  },
  {
    id: 'security-policies',
    type: 'GIT',
    uriOrPath: 'https://github.com/acme/security-policies.git',
    ref: 'main',
    include: [],
    ignore: [],
    name: 'Security Policies',
    description: 'Access control, incident disclosure and audit policies.',
  },
  {
    id: 'marketing-assets',
    type: 'FS',
    uriOrPath: '/srv/knowledge/marketing',
    ref: null,
    include: ['**/*.md'],
    ignore: ['drafts/**'],
    name: 'Marketing Assets',
    description: 'Brand guidelines and campaign briefs.',
  },
]

export const mockSources: Source[] = mockSourceBases.map((base, index) => ({
  ...base,
  updatedAt: mockUpdatedAt(index),
}))

/** Index freshness for a mock source — mirrors its `updatedAt` so the two
 *  never disagree. */
export function mockStatusFor(id: string): SourceStatus {
  const source = mockSources.find((s) => s.id === id)

  if (!source || !source.updatedAt) {
    return { sourceId: id, indexed: false, indexedAt: null, commitSha: null, ref: null }
  }

  return {
    sourceId: id,
    indexed: true,
    indexedAt: source.updatedAt,
    commitSha: source.type === 'GIT' ? '9f3c1a7e5b2d8c4f6a0e1b3d5c7a9f2e4b6d8c0a' : null,
    ref: source.ref,
  }
}

export const mockSyncResult: SyncResult = {
  sourceId: 'engineering-wiki',
  indexed: 4,
  reindexed: 2,
  evicted: 1,
  skipped: 37,
  commitsIndexed: 6,
  durationMs: 1840,
  toCommit: '9f3c1a7e5b2d8c4f6a0e1b3d5c7a9f2e4b6d8c0a',
  idempotent: false,
}
