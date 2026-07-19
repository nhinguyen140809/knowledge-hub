import type { RankedResult } from '../types/query.type'

/** A sample ranked result so the query UI can be explored without a backend. */
export const mockRankedResult: RankedResult = {
  servedFromCanonicalRef: false,
  hits: [
    {
      id: 'chunk_7f2a91',
      relevanceScore: 0.912,
      metadata: {
        kind: 'chunk',
        sourceId: 'engineering-wiki',
        path: 'retrieval/caching.md',
        lineStart: 14,
        lineEnd: 38,
        type: 'doc',
        ref: 'main',
        indexedAt: '2026-07-17T08:42:11Z',
        commitSha: '9f3c1a7e5b2d8c4f6a0e1b3d5c7a9f2e4b6d8c0a',
        viaPath: [],
      },
    },
    {
      id: 'chunk_1c8e40',
      relevanceScore: 0.774,
      metadata: {
        kind: 'chunk',
        sourceId: 'product-docs',
        path: 'api/query.md',
        lineStart: 3,
        lineEnd: 21,
        type: 'doc',
        ref: 'release',
        indexedAt: '2026-07-16T19:05:00Z',
        commitSha: null,
        viaPath: ['MENTIONS'],
      },
    },
    {
      id: 'entity_cache_key',
      relevanceScore: 0.658,
      metadata: {
        kind: 'entity',
        sourceId: 'engineering-wiki',
        path: 'retrieval/RetrievalCache.java',
        lineStart: null,
        lineEnd: null,
        type: 'code',
        ref: 'main',
        indexedAt: '2026-07-17T08:42:11Z',
        commitSha: '9f3c1a7e5b2d8c4f6a0e1b3d5c7a9f2e4b6d8c0a',
        viaPath: ['DEFINES', 'REFERENCES'],
      },
    },
  ],
}
