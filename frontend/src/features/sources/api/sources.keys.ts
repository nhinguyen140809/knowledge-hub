/**
 * Query-key factory for source queries. Every key is scoped by connection id so
 * switching backends never serves another instance's cache, and `all` lets Sync
 * invalidate the whole namespace in one call.
 */
export const sourceKeys = {
  all: ['sources'] as const,
  list: (connectionId: string | undefined) => [...sourceKeys.all, 'list', connectionId] as const,
  detail: (connectionId: string | undefined, id: string) =>
    [...sourceKeys.all, 'detail', connectionId, id] as const,
  status: (connectionId: string | undefined, id: string) =>
    [...sourceKeys.all, 'status', connectionId, id] as const,
}
