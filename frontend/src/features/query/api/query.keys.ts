import type { QueryInput } from '../types/query.type'

/**
 * Query-key factory for retrieval. The whole input is part of the key, so two
 * identical searches share one cache entry while changing any knob (topK,
 * sourceId, ref, type) is a distinct query.
 */
export const queryKeys = {
  all: ['query'] as const,
  search: (connectionId: string | undefined, input: QueryInput | null) =>
    [...queryKeys.all, 'search', connectionId, input] as const,
}
