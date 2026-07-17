/** Query-key factory for source queries. Centralised so pages/hooks share the
 *  exact keys and Sync can invalidate the whole namespace with `sourceKeys.all`. */
export const sourceKeys = {
  all: ['sources'] as const,
  list: (connectionId: string | undefined) => [...sourceKeys.all, 'list', connectionId] as const,
}
