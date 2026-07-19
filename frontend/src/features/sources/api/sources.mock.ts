import type { Source, SourceStatus, SyncResult } from '../types/source.type'

/** Sample sources so the UI can be explored in mock mode without a backend. */
export const mockSources: Source[] = [
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
]

export const mockSourceStatus: SourceStatus = {
  sourceId: 'engineering-wiki',
  indexed: true,
  indexedAt: '2026-07-17T08:42:11Z',
  commitSha: '9f3c1a7e5b2d8c4f6a0e1b3d5c7a9f2e4b6d8c0a',
  ref: 'main',
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
