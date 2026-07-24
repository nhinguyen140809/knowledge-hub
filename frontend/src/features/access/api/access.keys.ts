/**
 * Query-key factory for access queries, scoped by connection id. Grouped under
 * one `all` root so a mutation anywhere in access (a grant, a revoke, a policy
 * change) can invalidate everything that might have shifted — effective
 * permissions depend on grants, membership and the default policy at once.
 */
export const accessKeys = {
  all: ['access'] as const,

  principals: (connectionId: string | undefined) =>
    [...accessKeys.all, 'principals', connectionId] as const,

  principalGraph: (connectionId: string | undefined) =>
    [...accessKeys.all, 'principal-graph', connectionId] as const,

  allCredentials: (connectionId: string | undefined) =>
    [...accessKeys.all, 'all-credentials', connectionId] as const,

  principal: (connectionId: string | undefined, id: string) =>
    [...accessKeys.all, 'principal', connectionId, id] as const,

  members: (connectionId: string | undefined, groupId: string) =>
    [...accessKeys.all, 'members', connectionId, groupId] as const,

  credentials: (connectionId: string | undefined, principalId: string) =>
    [...accessKeys.all, 'credentials', connectionId, principalId] as const,

  effectivePermissions: (connectionId: string | undefined, principalId: string) =>
    [...accessKeys.all, 'effective-permissions', connectionId, principalId] as const,

  accessGraph: (connectionId: string | undefined, principalId: string) =>
    [...accessKeys.all, 'access-graph', connectionId, principalId] as const,

  defaultPolicy: (connectionId: string | undefined) =>
    [...accessKeys.all, 'default-policy', connectionId] as const,
}
